#!/usr/bin/env python3
"""
OpenClaw Gateway RPC 客戶端
直接與 Gateway WebSocket 通訊，避免 CLI 指令的緩衝問題
"""

import asyncio
import websockets
import json
import logging
import uuid
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class OpenClawGatewayClient:
    """OpenClaw Gateway WebSocket RPC 客戶端"""
    
    def __init__(self, url="ws://localhost:19001"):
        self.url = url
        self.ws = None
        self.responses = {}
        
    async def connect(self):
        """連接 Gateway"""
        try:
            self.ws = await websockets.connect(self.url)
            logger.info(f"✅ 連接到 Gateway：{self.url}")
            return True
        except Exception as e:
            logger.error(f"❌ 連接失敗：{e}")
            return False
    
    async def disconnect(self):
        """斷開連接"""
        if self.ws:
            await self.ws.close()
            logger.info("已斷開 Gateway 連接")
    
    async def call_method(self, method, params=None, timeout=30):
        """調用 Gateway RPC 方法"""
        if not self.ws:
            raise Exception("未連接到 Gateway")
        
        request_id = str(uuid.uuid4())
        request = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
            "params": params or {}
        }
        
        # 發送請求
        await self.ws.send(json.dumps(request))
        logger.info(f"📤 發送請求：{method}")
        
        # 接收回應（帶超時）
        try:
            response_text = await asyncio.wait_for(
                self.ws.recv(),
                timeout=timeout
            )
            response = json.loads(response_text)
            logger.info(f"📥 收到回應：{method}")
            return response
        except asyncio.TimeoutError:
            logger.error(f"⏱️ 請求超時：{method}")
            return {"error": "Request timeout"}
    
    async def send_agent_message(self, agent_id, message, session_id=None):
        """發送訊息到 Agent"""
        params = {
            "agentId": agent_id,
            "message": message
        }
        
        if session_id:
            params["sessionId"] = session_id
        
        return await self.call_method("agent.run", params, timeout=60)


async def test_gateway():
    """測試 Gateway 連接與訊息發送"""
    client = OpenClawGatewayClient()
    
    if not await client.connect():
        return
    
    try:
        # 測試 health
        health = await client.call_method("health")
        print(f"\n✅ Gateway Health：{health}\n")
        
        # 測試發送訊息
        response = await client.send_agent_message(
            agent_id="main",
            message="Hi, 測試訊息",
            session_id=f"mobile_test_{int(datetime.now().timestamp())}"
        )
        print(f"\n📨 Agent 回應：{json.dumps(response, indent=2, ensure_ascii=False)}\n")
        
    finally:
        await client.disconnect()


if __name__ == "__main__":
    asyncio.run(test_gateway())
