/**
 * Auto-Categorize Popup Component
 * MoMo-style AI category suggestion popup
 * Shows top 3 category suggestions with confidence scores
 */

class AutoCategorizePopup {
    constructor() {
        this.popup = null;
        this.currentSuggestions = null;
        this.onSelectCallback = null;
        this.init();
    }

    init() {
        // Create popup HTML structure
        const popupHTML = `
            <div id="auto-categorize-popup" class="modal fade" tabindex="-1" role="dialog">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header bg-primary text-white">
                            <h5 class="modal-title">
                                <i class="fas fa-magic me-2"></i>
                                Gợi ý phân loại thông minh
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div id="categorize-loading" class="text-center py-4" style="display: none;">
                                <div class="spinner-border text-primary" role="status">
                                    <span class="visually-hidden">Đang phân tích...</span>
                                </div>
                                <p class="mt-3 text-muted">AI đang phân tích giao dịch...</p>
                            </div>
                            
                            <div id="categorize-suggestions" style="display: none;">
                                <div class="alert alert-info mb-3">
                                    <i class="fas fa-info-circle me-2"></i>
                                    <span id="ai-reasoning"></span>
                                </div>
                                
                                <h6 class="mb-3">Chọn danh mục phù hợp:</h6>
                                <div id="suggestion-list" class="d-grid gap-2">
                                    <!-- Suggestions will be inserted here -->
                                </div>
                            </div>
                            
                            <div id="categorize-error" class="alert alert-danger" style="display: none;">
                                <i class="fas fa-exclamation-triangle me-2"></i>
                                <span id="error-message"></span>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                Bỏ qua
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Append to body if not exists
        if (!document.getElementById('auto-categorize-popup')) {
            document.body.insertAdjacentHTML('beforeend', popupHTML);
        }

        this.popup = new bootstrap.Modal(document.getElementById('auto-categorize-popup'));
    }

    /**
     * Show popup with AI category suggestions
     * @param {string} description - Transaction description
     * @param {number} amount - Transaction amount
     * @param {Function} onSelect - Callback when user selects a category
     */
    async show(description, amount, onSelect) {
        this.onSelectCallback = onSelect;
        
        // Show popup with loading state
        document.getElementById('categorize-loading').style.display = 'block';
        document.getElementById('categorize-suggestions').style.display = 'none';
        document.getElementById('categorize-error').style.display = 'none';
        
        this.popup.show();

        try {
            // Call AI API
            const result = await this.fetchCategorizeSuggestions(description, amount);
            
            // Debug logging
            console.log('=== AUTO CATEGORIZE DEBUG ===');
            console.log('Full result:', result);
            console.log('All keys:', Object.keys(result));
            console.log('result.success:', result.success);
            console.log('result["autoCategorize categorized"]:', result["autoCategorize categorized"]);
            
            // Check if we have suggestions (don't rely on the weird field name)
            const hasValidSuggestions = result.suggestions && Array.isArray(result.suggestions) && result.suggestions.length > 0;
            
            console.log('hasValidSuggestions:', hasValidSuggestions);
            
            // Just check if we have suggestions array with data
            if (hasValidSuggestions) {
                console.log('✅ Displaying suggestions');
                this.displaySuggestions(result);
            } else {
                console.log('❌ No valid suggestions - showing error');
                this.showError('Không thể tạo gợi ý phân loại');
            }
        } catch (error) {
            console.error('Auto-categorize error:', error);
            this.showError(error.message || 'Lỗi kết nối đến AI service');
        } finally {
            document.getElementById('categorize-loading').style.display = 'none';
        }
    }

    async fetchCategorizeSuggestions(description, amount) {
        const token = localStorage.getItem('authToken');
        
        const response = await fetch('/api/ai/auto-categorize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                description: description,
                amount: amount
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    }

    displaySuggestions(result) {
        this.currentSuggestions = result;
        
        // Update reasoning
        document.getElementById('ai-reasoning').textContent = result.reasoning || result.message;
        
        // Build suggestion buttons
        const suggestionList = document.getElementById('suggestion-list');
        suggestionList.innerHTML = '';

        result.suggestions.forEach((suggestion, index) => {
            const confidence = Math.round(suggestion.confidence * 100);
            const isPrimary = index === 0;
            
            const buttonHTML = `
                <button type="button" 
                        class="btn ${isPrimary ? 'btn-primary' : 'btn-outline-primary'} text-start suggestion-btn"
                        data-category-id="${suggestion.id}"
                        data-category-name="${suggestion.name}"
                        data-confidence="${suggestion.confidence}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <span class="fw-bold">${suggestion.name}</span>
                            ${isPrimary ? '<span class="badge bg-success ms-2">Gợi ý tốt nhất</span>' : ''}
                        </div>
                        <div>
                            <span class="badge bg-light text-dark">${confidence}%</span>
                        </div>
                    </div>
                    <div class="progress mt-2" style="height: 4px;">
                        <div class="progress-bar ${isPrimary ? 'bg-success' : 'bg-primary'}" 
                             style="width: ${confidence}%"></div>
                    </div>
                </button>
            `;
            
            suggestionList.insertAdjacentHTML('beforeend', buttonHTML);
        });

        // Add click handlers
        suggestionList.querySelectorAll('.suggestion-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const categoryId = btn.dataset.categoryId;
                const categoryName = btn.dataset.categoryName;
                const confidence = parseFloat(btn.dataset.confidence);
                
                this.selectCategory(categoryId, categoryName, confidence);
            });
        });

        // Show suggestions
        document.getElementById('categorize-suggestions').style.display = 'block';
    }

    selectCategory(categoryId, categoryName, confidence) {
        // Close popup
        this.popup.hide();
        
        // Callback with selected category
        if (this.onSelectCallback) {
            this.onSelectCallback({
                id: categoryId,
                name: categoryName,
                confidence: confidence
            });
        }
        
        // Show success toast
        this.showToast(`✓ Đã chọn "${categoryName}" (${Math.round(confidence * 100)}% phù hợp)`, 'success');
    }

    showError(message) {
        document.getElementById('error-message').textContent = message;
        document.getElementById('categorize-error').style.display = 'block';
    }

    showToast(message, type = 'info') {
        // Create toast notification
        const toastHTML = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        // Create toast container if not exists
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(container);
        }
        
        container.insertAdjacentHTML('beforeend', toastHTML);
        const toastEl = container.lastElementChild;
        const toast = new bootstrap.Toast(toastEl);
        toast.show();
        
        // Remove after hidden
        toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    }

    hide() {
        this.popup.hide();
    }
}

// Export as singleton - initialize when DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        window.AutoCategorizePopup = window.AutoCategorizePopup || new AutoCategorizePopup();
    });
} else {
    // DOM already loaded
    window.AutoCategorizePopup = window.AutoCategorizePopup || new AutoCategorizePopup();
}
