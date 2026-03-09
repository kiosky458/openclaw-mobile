#!/usr/bin/env python3
"""
OpenClaw Mobile Server - WebSocket 版本
使用 Flask-SocketIO 實現即時雙向通訊
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_socketio import SocketIO, emit, join_room, leave_room
import subprocess
import logging
import time
import json
import os
from datetime import datetime
from threading import Thread

app = Flask(__name__)
app.config['SECRET_KEY'] = 'openclaw-mobile-secret-2026'
CORS(app, resources={r"/*": {"origins": "*"}})

# 初始化 SocketIO
socketio = SocketIO(
    app,
    cors_allowed_origins="*",
    logger=True,
    engineio_logger=False,
    ping_timeout=60,
    ping_interval=25
)

# 設定日誌
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)

# OpenClaw CLI 路徑
OPENCLAW_BIN = '/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw'

# 全域變數
connected_devices = {}  # {device_id: sid}
active_conversations = {}  # {device_id: {'messages': [], 'last_activity': timestamp}}

# ==================== WebSocket 事件 ====================

@socketio.on('connect')
def handle_connect():
    """客戶端連接"""
    logging.info(f"🔌 新連接：{request.sid}")
    emit('connected', {'status': 'ok', 'message': '已連接到 OpenClaw Server'})

@socketio.on('disconnect')
def handle_disconnect():
    """客戶端斷線"""
    # 找到並移除裝置
    device_id = None
    for did, sid in list(connected_devices.items()):
        if sid == request.sid:
            device_id = did
            del connected_devices[did]
            break
    
    if device_id:
        logging.info(f"🔌 裝置斷線：{device_id}")
    else:
        logging.info(f"🔌 未知連接斷線：{request.sid}")

@socketio.on('register')
def handle_register(data):
    """註冊裝置（支持重連後推送未讀消息）"""
    device_id = data.get('device_id')
    last_message_id = data.get('last_message_id', 0)  # 客戶端最後收到的消息 ID
    
    if not device_id:
        emit('error', {'message': '缺少 device_id'})
        return
    
    # 記錄裝置與連接的對應
    connected_devices[device_id] = request.sid
    
    # 加入專屬 room
    join_room(device_id)
    
    logging.info(f"📱 裝置已註冊：{device_id} (sid={request.sid}, last_msg_id={last_message_id})")
    emit('registered', {'device_id': device_id, 'status': 'ok'})
    
    # 🔄 推送未讀消息（握手機制）
    if device_id in active_conversations:
        messages = active_conversations[device_id]['messages']
        
        # 過濾出新消息（ID 大於 last_message_id）
        unread_messages = [
            msg for msg in messages
            if msg['id'] > last_message_id
        ]
        
        if unread_messages:
            logging.info(f"📬 推送 {len(unread_messages)} 條未讀訊息到 {device_id}")
            
            # 逐條推送（避免一次性太多）
            for msg in unread_messages:
                emit('new_message', {'message': msg}, room=device_id)
                time.sleep(0.05)  # 50ms 間隔
            
            # 發送同步完成通知
            emit('sync_done', {
                'status': 'ok',
                'message_count': len(unread_messages)
            }, room=device_id)
@socketio.on('send_message')
def handle_send_message(data):
    """處理發送訊息"""
    device_id = data.get('device_id')
    agent_id = data.get('agent_id', 'spark')
    message = data.get('message', '')
    
    if not device_id or not message:
        emit('error', {'message': '缺少 device_id 或 message'})
        return
    
    logging.info(f"📨 收到訊息：{device_id} → {agent_id}：{message}")
    
    # 發送確認
    emit('message_received', {'status': 'ok', 'message': '訊息已收到'})
    
    # 在背景執行 Agent 處理
    thread = Thread(target=process_agent_message_websocket, args=(device_id, agent_id, message))
    thread.daemon = True
    thread.start()

# ==================== Agent 處理（WebSocket 版本）====================

def process_agent_message_websocket(device_id, agent_id, message):
    """處理 Agent 訊息（WebSocket 推送版本）"""
    try:
        # 根據 agent_id 選擇 agent
        agent_map = {
            'spark': 'main',
            'data': 'main',
            'numberone': 'main'
        }
        agent = agent_map.get(agent_id, 'main')
        
        # 使用獨立的 session ID
        session_id = f"mobile_{device_id}_{int(time.time())}"
        
        # 使用 openclaw agent 指令
        cmd = [OPENCLAW_BIN, 'agent', '--agent', agent, '--session-id', session_id, '--message', message]
        
        logging.info(f"🚀 執行指令：{' '.join(cmd)}")
        
        # 先用簡單的 communicate() 方式（穩定版）
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        if device_id not in active_conversations:
            active_conversations[device_id] = {'messages': [], 'last_activity': time.time()}
        
        conversation = active_conversations[device_id]
        
        try:
            stdout, stderr = process.communicate(timeout=120)
            
            message_count = 0
            
            # 處理輸出並逐行推送
            if stdout:
                for line in stdout.splitlines():
                    line = line.strip()
                    # 過濾掉 OpenClaw 的 banner 和日誌訊息
                    if line and not line.startswith('🦞') and not line.startswith('[') and not line.startswith('error:'):
                        # 建立訊息對象
                        msg = {
                            'id': time.time(),
                            'content': line,
                            'from': 'agent',
                            'timestamp': str(datetime.now()),
                            'streaming': False
                        }
                        
                        # 添加到對話歷史
                        conversation['messages'].append(msg)
                        conversation['last_activity'] = time.time()
                        message_count += 1
                        
                        # 推送到客戶端
                        if device_id in connected_devices:
                            socketio.emit('new_message', {'message': msg}, room=device_id)
                            logging.info(f"📤 推送訊息：{line[:50]}...")
                        
                        # 避免推送過快
                        time.sleep(0.05)
            
            # 如果沒有任何輸出，檢查 stderr
            if message_count == 0 and stderr:
                logging.warning(f"⚠️ Agent stderr：{stderr[:500]}")
                error_msg = {
                    'id': time.time(),
                    'content': f'Agent 錯誤：{stderr[:500]}',
                    'from': 'system',
                    'timestamp': str(datetime.now())
                }
                conversation['messages'].append(error_msg)
                
                if device_id in connected_devices:
                    socketio.emit('new_message', {'message': error_msg}, room=device_id)
            
            # 發送完成通知
            if device_id in connected_devices:
                socketio.emit('agent_done', {
                    'status': 'ok',
                    'message_count': message_count
                }, room=device_id)
            
            logging.info(f"✅ {agent_id} 回應完成：{message_count} 條訊息（WebSocket 推送）")
            
        except subprocess.TimeoutExpired:
            process.kill()
            logging.error(f"⏱️ Agent 執行超時（120 秒）")
            
            error_msg = {
                'id': time.time(),
                'content': 'Agent 回應超時（超過 120 秒），請稍後再試',
                'from': 'system',
                'timestamp': str(datetime.now())
            }
            
            if device_id in active_conversations:
                active_conversations[device_id]['messages'].append(error_msg)
            
            if device_id in connected_devices:
                socketio.emit('new_message', {'message': error_msg}, room=device_id)
        
    except Exception as e:
        logging.error(f"❌ Agent 通訊錯誤：{e}")
        
        error_msg = {
            'id': time.time(),
            'content': f'錯誤：{str(e)}',
            'from': 'system',
            'timestamp': str(datetime.now())
        }
        
        if device_id in active_conversations:
            active_conversations[device_id]['messages'].append(error_msg)
        
        if device_id in connected_devices:
            socketio.emit('new_message', {'message': error_msg}, room=device_id)
# ==================== HTTP API（保留用於測試）====================

@app.route('/api/health', methods=['GET'])
def health():
    """健康檢查"""
    return jsonify({
        'status': 'ok',
        'mode': 'websocket',
        'connected_devices': len(connected_devices),
        'active_conversations': len(active_conversations)
    })

@app.route('/api/sessions', methods=['GET'])
def get_sessions():
    """獲取所有 OpenClaw Sessions"""
    try:
        result = subprocess.run(
            [OPENCLAW_BIN, 'sessions', '--json', '--all-agents'],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            sessions = json.loads(result.stdout)
            return jsonify({'sessions': sessions})
        else:
            return jsonify({'error': result.stderr}), 500
            
    except Exception as e:
        logging.error(f"❌ 獲取 sessions 失敗：{e}")
        return jsonify({'error': str(e)}), 500

# ==================== 啟動 Server ====================

if __name__ == '__main__':
    logging.info("🚀 OpenClaw Mobile Server（WebSocket 版本）啟動")
    logging.info(f"📍 OpenClaw CLI：{OPENCLAW_BIN}")
    
    # 使用 SocketIO 運行（支持 WebSocket）
    socketio.run(
        app,
        host='0.0.0.0',
        port=5001,
        debug=True,
        allow_unsafe_werkzeug=True
    )
