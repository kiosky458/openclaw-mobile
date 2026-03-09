#!/usr/bin/env python3
"""
OpenClaw Mobile Server - HTTP API 後端
整合 OpenClaw CLI 工具，提供 Dashboard 功能
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import subprocess
import logging
import time
import json
import os
import psutil
from datetime import datetime
from threading import Thread

app = Flask(__name__)
CORS(app)

# 設定日誌
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)

# OpenClaw CLI 路徑
OPENCLAW_BIN = '/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw'

# 全域變數
active_conversations = {}  # {device_id: {'messages': [], 'last_activity': timestamp}}


# ============================================================================
# 基礎 Chat API（與 android-stream-relay 相同）
# ============================================================================

@app.route('/api/mobile/register', methods=['POST'])
def mobile_register():
    """裝置註冊"""
    data = request.get_json() or {}
    device_id = data.get('device_id', 'unknown')
    
    active_conversations[device_id] = {
        'messages': [],
        'last_activity': time.time()
    }
    
    logging.info(f"📱 Mobile 裝置已註冊：{device_id}")
    
    return jsonify({
        'status': 'ok',
        'device_id': device_id,
        'server_time': str(datetime.now())
    })


@app.route('/api/mobile/send', methods=['POST'])
def mobile_send():
    """發送訊息到 Agent"""
    data = request.get_json() or {}
    device_id = data.get('device_id', 'unknown')
    agent_id = data.get('agent_id', 'spark')
    message = data.get('message', '').strip()
    
    if not message:
        return jsonify({'error': '訊息不能為空'}), 400
    
    logging.info(f"📨 Mobile 訊息：{device_id} → {agent_id}：{message}")
    
    # 在背景執行緒處理
    thread = Thread(
        target=process_agent_message,
        args=(device_id, agent_id, message)
    )
    thread.daemon = True
    thread.start()
    
    return jsonify({'status': 'ok', 'message_id': str(time.time())})


@app.route('/api/mobile/poll', methods=['POST'])
def mobile_poll():
    """輪詢新訊息"""
    data = request.get_json() or {}
    device_id = data.get('device_id', 'unknown')
    last_message_id = data.get('last_message_id', 0)
    
    if device_id not in active_conversations:
        logging.debug(f"📭 輪詢：{device_id} 無對話記錄")
        return jsonify({'messages': []})
    
    conversation = active_conversations[device_id]
    conversation['last_activity'] = time.time()
    
    # 返回新訊息
    new_messages = [
        msg for msg in conversation['messages']
        if msg['id'] > last_message_id
    ]
    
    # 調試日誌
    total_msgs = len(conversation['messages'])
    new_count = len(new_messages)
    logging.info(f"📬 輪詢：{device_id[:20]}... last_id={last_message_id:.2f} 總訊息={total_msgs} 新訊息={new_count}")
    
    return jsonify({'messages': new_messages})


# ============================================================================
# Dashboard API（OpenClaw 核心功能）
# ============================================================================

@app.route('/api/dashboard/sessions', methods=['GET'])
def get_sessions():
    """列出所有 OpenClaw Sessions"""
    try:
        # 執行 openclaw sessions --json --all-agents
        cmd = [OPENCLAW_BIN, 'sessions', '--json', '--all-agents']
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            sessions_data = json.loads(result.stdout)
            return jsonify({'sessions': sessions_data})
        else:
            logging.error(f"OpenClaw sessions 失敗：{result.stderr}")
            return jsonify({'error': 'Failed to list sessions', 'details': result.stderr}), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Request timeout'}), 504
    except json.JSONDecodeError as e:
        logging.error(f"JSON 解析失敗：{e}")
        return jsonify({'error': 'Invalid JSON response', 'details': str(e)}), 500
    except Exception as e:
        logging.error(f"Sessions list 錯誤：{e}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/dashboard/session_status/<path:session_key>', methods=['GET'])
def get_session_status(session_key):
    """取得單一 Session 狀態"""
    try:
        # 執行 openclaw tool session_status --sessionKey={session_key}
        cmd = [OPENCLAW_BIN, 'tool', 'session_status', '--sessionKey', session_key, '--json']
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            status = json.loads(result.stdout)
            
            # 加入系統資訊
            status['system'] = get_system_info()
            
            return jsonify(status)
        else:
            logging.error(f"Session status 失敗：{result.stderr}")
            return jsonify({'error': 'Failed to get session status'}), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Request timeout'}), 504
    except Exception as e:
        logging.error(f"Session status 錯誤：{e}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/dashboard/sessions/send', methods=['POST'])
def send_to_session():
    """發送訊息到指定 Session"""
    data = request.get_json() or {}
    session_key = data.get('session_key', '')
    message = data.get('message', '').strip()
    
    if not session_key or not message:
        return jsonify({'error': 'session_key and message are required'}), 400
    
    try:
        # 執行 openclaw tool sessions_send --sessionKey={session_key} --message="{message}"
        cmd = [OPENCLAW_BIN, 'tool', 'sessions_send', '--sessionKey', session_key, '--message', message]
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            return jsonify({
                'status': 'ok',
                'session_key': session_key,
                'message_id': str(time.time())
            })
        else:
            logging.error(f"Sessions send 失敗：{result.stderr}")
            return jsonify({'error': 'Failed to send message'}), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Request timeout'}), 504
    except Exception as e:
        logging.error(f"Sessions send 錯誤：{e}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/dashboard/subagents', methods=['POST'])
def control_subagents():
    """控制 Sub-Agents（列出、殺死、調度）"""
    data = request.get_json() or {}
    action = data.get('action', 'list')
    session_key = data.get('session_key', '')
    
    try:
        if action == 'list':
            # 執行 openclaw tool subagents --action=list
            cmd = [OPENCLAW_BIN, 'tool', 'subagents', '--action', 'list', '--json']
            
        elif action == 'kill':
            if not session_key:
                return jsonify({'error': 'session_key is required for kill action'}), 400
            
            # 執行 openclaw tool subagents --action=kill --target={session_key}
            cmd = [OPENCLAW_BIN, 'tool', 'subagents', '--action', 'kill', '--target', session_key]
            
        else:
            return jsonify({'error': f'Unknown action: {action}'}), 400
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            if action == 'list':
                subagents = json.loads(result.stdout)
                return jsonify({'subagents': subagents})
            else:
                return jsonify({
                    'status': 'ok',
                    'session_key': session_key,
                    'action': action
                })
        else:
            logging.error(f"Subagents {action} 失敗：{result.stderr}")
            return jsonify({'error': f'Failed to {action} subagents'}), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Request timeout'}), 504
    except Exception as e:
        logging.error(f"Subagents {action} 錯誤：{e}")
        return jsonify({'error': str(e)}), 500


# ============================================================================
# 輔助函數
# ============================================================================

def process_agent_message(device_id, agent_id, message):
    """處理 Agent 訊息（背景執行，實時流式輸出）"""
    try:
        # 根據 agent_id 選擇 agent（目前只有 main agent）
        agent_map = {
            'spark': 'main',
            'data': 'main',
            'numberone': 'main'
        }
        agent = agent_map.get(agent_id, 'main')
        
        # 使用獨立的 session ID 避免鎖定衝突
        session_id = f"mobile_{device_id}_{int(time.time())}"
        
        # 使用 openclaw agent 指令
        cmd = [OPENCLAW_BIN, 'agent', '--agent', agent, '--session-id', session_id, '--message', message]
        
        logging.info(f"🚀 執行指令：{' '.join(cmd)}")
        
        # 使用 Popen 實時讀取輸出
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1  # 行緩衝
        )
        
        if device_id not in active_conversations:
            active_conversations[device_id] = {'messages': [], 'last_activity': time.time()}
        
        conversation = active_conversations[device_id]
        message_count_before = len(conversation['messages'])
        
        # 實時逐行讀取 stdout
        try:
            for line in iter(process.stdout.readline, ''):
                if not line:
                    break
                
                line = line.rstrip()
                
                # 過濾掉 OpenClaw 的 banner 和日誌訊息
                if line and not line.startswith('🦞') and not line.startswith('[') and not line.startswith('error:'):
                    # 立即添加到消息隊列（實現流式效果）
                    conversation['messages'].append({
                        'id': time.time(),
                        'content': line,
                        'from': 'agent',
                        'timestamp': str(datetime.now()),
                        'streaming': True  # 標記為流式消息
                    })
                    conversation['last_activity'] = time.time()
            
            # 等待進程結束（最多 120 秒）
            process.wait(timeout=120)
            
            # 如果沒有任何輸出，檢查 stderr
            if len(conversation['messages']) == message_count_before:
                stderr = process.stderr.read()
                if stderr:
                    logging.warning(f"⚠️ Agent stderr：{stderr[:500]}")
                    conversation['messages'].append({
                        'id': time.time(),
                        'content': f'Agent 錯誤：{stderr[:500]}',
                        'from': 'system',
                        'timestamp': str(datetime.now())
                    })
            
            new_messages = len(conversation['messages']) - message_count_before
            logging.info(f"✅ {agent_id} 回應完成：{new_messages} 條新訊息（流式）")
            
        except subprocess.TimeoutExpired:
            process.kill()
            logging.error(f"⏱️ Agent 執行超時（120 秒）")
            if device_id in active_conversations:
                active_conversations[device_id]['messages'].append({
                    'id': time.time(),
                    'content': 'Agent 回應超時（超過 120 秒），請稍後再試',
                    'from': 'system',
                    'timestamp': str(datetime.now())
                })
        
    except Exception as e:
        logging.error(f"❌ Agent 通訊錯誤：{e}")
        if device_id in active_conversations:
            active_conversations[device_id]['messages'].append({
                'id': time.time(),
                'content': f'錯誤：{str(e)}',
                'from': 'system',
                'timestamp': str(datetime.now())
            })
def get_system_info():
    """取得系統資訊（CPU、記憶體、溫度）"""
    try:
        cpu_percent = psutil.cpu_percent(interval=1)
        memory = psutil.virtual_memory()
        
        # GPU 資訊（需要 nvidia-smi）
        gpu_percent = 0
        temp_gpu = 0
        try:
            result = subprocess.run(
                ['nvidia-smi', '--query-gpu=utilization.gpu,temperature.gpu', '--format=csv,noheader,nounits'],
                capture_output=True,
                text=True,
                timeout=2
            )
            if result.returncode == 0:
                parts = result.stdout.strip().split(',')
                gpu_percent = float(parts[0].strip())
                temp_gpu = int(parts[1].strip())
        except:
            pass
        
        # CPU 溫度（Linux）
        temp_cpu = 0
        try:
            if os.path.exists('/sys/class/thermal/thermal_zone0/temp'):
                with open('/sys/class/thermal/thermal_zone0/temp', 'r') as f:
                    temp_cpu = int(f.read().strip()) // 1000
        except:
            pass
        
        return {
            'cpu_percent': round(cpu_percent, 1),
            'memory_mb': memory.used // (1024 * 1024),
            'gpu_percent': round(gpu_percent, 1),
            'temp_cpu': temp_cpu,
            'temp_gpu': temp_gpu
        }
    except Exception as e:
        logging.error(f"取得系統資訊失敗：{e}")
        return {
            'cpu_percent': 0,
            'memory_mb': 0,
            'gpu_percent': 0,
            'temp_cpu': 0,
            'temp_gpu': 0
        }


# ============================================================================
# 主程式
# ============================================================================

if __name__ == '__main__':
    logging.info("🚀 OpenClaw Mobile Server 啟動")
    logging.info(f"📍 OpenClaw CLI：{OPENCLAW_BIN}")
    
    # 檢查 OpenClaw CLI 是否存在
    if not os.path.exists(OPENCLAW_BIN):
        logging.error(f"❌ OpenClaw CLI 不存在：{OPENCLAW_BIN}")
        exit(1)
    
    # 啟動 Flask（開發模式）
    app.run(
        host='0.0.0.0',
        port=5001,
        debug=True
    )
