# 🚀 OpenRouter AI Setup Guide

## **Bước 1: Lấy API Key**

1. Truy cập [OpenRouter](https://openrouter.ai/)
2. Đăng ký tài khoản miễn phí
3. Vào [API Keys](https://openrouter.ai/keys)
4. Tạo API key mới

## **Bước 2: Cấu hình Environment Variables**

Tạo file `.env` trong thư mục `backend/`:

```bash
# OpenRouter Configuration
OPENROUTER_API_KEY=your_api_key_here
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_MODEL=anthropic/claude-3.5-sonnet

# AI Provider
AI_PROVIDER=openrouter
```

## **Bước 3: Các Model Available**

OpenRouter cung cấp nhiều model mạnh mẽ:

### **Free Models:**
- `anthropic/claude-3.5-sonnet` (Claude 3.5 Sonnet)
- `deepseek/deepseek-r1-distill-llama-70b` (DeepSeek R1)
- `meta-llama/llama-3.1-8b-instruct` (Llama 3.1 8B)

### **Premium Models:**
- `anthropic/claude-3.5-sonnet` (Claude 3.5 Sonnet)
- `openai/gpt-4o` (GPT-4 Omni)
- `google/gemini-pro` (Gemini Pro)

## **Bước 4: Test API**

Sau khi cấu hình, restart backend và test:

```bash
curl -X GET http://localhost:8080/api/chat/status
```

Response mong đợi:
```json
{
  "success": true,
  "status": {
    "provider": "openrouter",
    "openrouter_available": true,
    "default_model": "anthropic/claude-3.5-sonnet",
    "timestamp": 1234567890
  }
}
```

## **Bước 5: Sử dụng trong Chat**

Chat sẽ tự động sử dụng OpenRouter AI với model đã cấu hình.

## **Lưu ý:**

- **Free Tier**: 10,000 requests/month
- **Rate Limits**: 10 requests/minute
- **Model Costs**: Free models có thể chậm hơn premium
- **Fallback**: Nếu OpenRouter offline, chat sẽ báo lỗi

## **Troubleshooting:**

1. **API Key Invalid**: Kiểm tra lại API key
2. **Rate Limit**: Đợi 1 phút rồi thử lại
3. **Model Unavailable**: Kiểm tra model name
4. **Network Error**: Kiểm tra kết nối internet
