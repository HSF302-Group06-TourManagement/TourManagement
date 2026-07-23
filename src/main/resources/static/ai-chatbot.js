(function () {
    'use strict';

    if (document.getElementById('ai-chatbot-launcher')) return;

    const widgetHtml = `
        <button id="ai-chatbot-launcher" title="Trợ lý AI TourVerse">
            <span class="material-symbols-outlined">smart_toy</span>
        </button>

        <div id="ai-chatbot-window">
            <div class="ai-chatbot-header">
                <div class="ai-chatbot-header-info">
                    <div class="ai-avatar">
                        <span class="material-symbols-outlined">smart_toy</span>
                        <span class="ai-status-dot"></span>
                    </div>
                    <div>
                        <h6 class="ai-header-title">Trợ lý AI TourVerse</h6>
                        <p class="ai-header-subtitle">Online | Đang kết nối Database</p>
                    </div>
                </div>
                <button id="ai-chatbot-close" class="ai-chatbot-close-btn" title="Đóng chat">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <div id="ai-chatbot-messages" class="ai-chatbot-messages">
                <div class="chat-msg chat-msg-ai">
                    <div class="chat-bubble">
                        Xin chào! Tôi là <strong>Trợ lý AI Du lịch TourVerse</strong>.
                        Tôi có thể tư vấn thông tin các tour du lịch đang có trong database. Bạn cần tìm tour đi đâu?
                    </div>
                </div>
            </div>

            <div class="ai-chatbot-suggestions" id="ai-suggestions">
                <button class="suggestion-pill">Các tour đi Đà Nẵng?</button>
                <button class="suggestion-pill">Tour giá rẻ nhất?</button>
                <button class="suggestion-pill">Danh sách tất cả tour</button>
            </div>

            <div class="ai-chatbot-footer">
                <form id="ai-chatbot-form" style="display: flex; width: 100%; gap: 8px; align-items: center; margin: 0;">
                    <input type="text" id="ai-chatbot-input-field" class="ai-chatbot-input" placeholder="Nhập câu hỏi của bạn..." autocomplete="off">
                    <button type="submit" id="ai-chatbot-send-btn" class="ai-chatbot-send-btn" title="Gửi tin nhắn">
                        <span class="material-symbols-outlined">send</span>
                    </button>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', widgetHtml);

    const launcher = document.getElementById('ai-chatbot-launcher');
    const windowEl = document.getElementById('ai-chatbot-window');
    const closeBtn = document.getElementById('ai-chatbot-close');
    const form = document.getElementById('ai-chatbot-form');
    const input = document.getElementById('ai-chatbot-input-field');
    const messages = document.getElementById('ai-chatbot-messages');
    const suggestions = document.getElementById('ai-suggestions');

    launcher.addEventListener('click', () => {
        windowEl.classList.toggle('active');
        if (windowEl.classList.contains('active')) {
            input.focus();
        }
    });

    closeBtn.addEventListener('click', () => {
        windowEl.classList.remove('active');
    });

    suggestions.addEventListener('click', (event) => {
        if (event.target.classList.contains('suggestion-pill')) {
            sendMessage(event.target.textContent);
        }
    });

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        const text = input.value.trim();
        if (text) {
            sendMessage(text);
            input.value = '';
        }
    });

    function sendMessage(text) {
        appendMessage('user', text);
        const typingId = appendTypingIndicator();
        scrollToBottom();

        fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8'
            },
            body: JSON.stringify({ message: text })
        })
            .then(response => response.json())
            .then(data => {
                removeTyping(typingId);
                appendMessage('ai', data.reply || 'Xin lỗi, tôi chưa thể trả lời lúc này.');
                if (data.suggestedQuestions && data.suggestedQuestions.length > 0) {
                    suggestions.innerHTML = data.suggestedQuestions
                        .map(question => `<button class="suggestion-pill">${escapeHtml(question)}</button>`)
                        .join('');
                }
                scrollToBottom();
            })
            .catch(error => {
                console.error('AI error:', error);
                removeTyping(typingId);
                appendMessage('ai', 'Không thể kết nối server AI.');
                scrollToBottom();
            });
    }

    function appendMessage(sender, text) {
        const msg = document.createElement('div');
        msg.className = `chat-msg chat-msg-${sender}`;
        msg.innerHTML = `<div class="chat-bubble">${formatMarkdown(text)}</div>`;
        messages.appendChild(msg);
    }

    function appendTypingIndicator() {
        const id = 'typing-' + Date.now();
        const msg = document.createElement('div');
        msg.id = id;
        msg.className = 'chat-msg chat-msg-ai';
        msg.innerHTML = `<div class="chat-bubble"><div class="typing-dots"><span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span></div></div>`;
        messages.appendChild(msg);
        return id;
    }

    function removeTyping(id) {
        const element = document.getElementById(id);
        if (element) element.remove();
    }

    function scrollToBottom() {
        messages.scrollTop = messages.scrollHeight;
    }

    function formatMarkdown(text) {
        if (!text) return '';
        let html = escapeHtml(text);
        html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\[status:active\](.*?)\[\/status\]/g, '<span class="ai-status-badge ai-status-active">$1</span>');
        html = html.replace(/\[status:inactive\](.*?)\[\/status\]/g, '<span class="ai-status-badge ai-status-inactive">$1</span>');
        html = html.replace(/\[status:draft\](.*?)\[\/status\]/g, '<span class="ai-status-badge ai-status-draft">$1</span>');
        html = html.replace(/^- (.*$)/gim, '&bull; $1');
        html = html.replace(/\n/g, '<br>');
        return html;
    }

    function escapeHtml(str) {
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
