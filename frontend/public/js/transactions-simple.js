// Auto-categorize feature for transactions (direct API call, no popup)// Auto-categorize feature for transactions (direct API call, no popup)

// This file adds AI auto-categorization to the transaction form// This file adds AI auto-categorization to the transaction form



document.addEventListener('DOMContentLoaded', function() {document.addEventListener('DOMContentLoaded', function() {

    addAIFeatures();    addAIFeatures();

});});



function addAIFeatures() {function addAIFeatures() {

    // Auto-categorize when description + amount are entered (MoMo-style)    // Auto-categorize when description + amount are entered (MoMo-style)

    const noteInput = document.querySelector('input[name="note"], textarea[name="note"]');    const noteInput = document.querySelector('input[name=""note""], textarea[name=""note""]');

    const amountInput = document.querySelector('input[name="amount"]');    const amountInput = document.querySelector('input[name=""amount""]');

        

    if (noteInput && amountInput) {    if (noteInput && amountInput) {

        let autoCategorizeTimeout;        let autoCategorizeTimeout;

                

        // Trigger on note input        // Trigger on note input

        noteInput.addEventListener('input', function() {        noteInput.addEventListener('input', function() {

            clearTimeout(autoCategorizeTimeout);            clearTimeout(autoCategorizeTimeout);

            const description = this.value.trim();            const description = this.value.trim();

            const amount = parseFloat(amountInput.value);            const amount = parseFloat(amountInput.value);

                        

            // Trigger AI if description > 3 chars and amount > 0            // Trigger AI if description > 3 chars and amount > 0

            if (description.length > 3 && amount > 0) {            if (description.length > 3 && amount > 0) {

                autoCategorizeTimeout = setTimeout(() => {                autoCategorizeTimeout = setTimeout(() => {

                    triggerAutoCategorize(description, amount);                    triggerAutoCategorize(description, amount);

                }, 1500); // Debounce 1.5s                }, 1500); // Debounce 1.5s

            }            }

        });        });

                

        // Also trigger when amount changes (if note already filled)        // Also trigger when amount changes (if note already filled)

        amountInput.addEventListener('blur', function() {        amountInput.addEventListener('blur', function() {

            const description = noteInput.value.trim();            const description = noteInput.value.trim();

            const amount = parseFloat(this.value);            const amount = parseFloat(this.value);

                        

            if (description.length > 3 && amount > 0) {            if (description.length > 3 && amount > 0) {

                setTimeout(() => triggerAutoCategorize(description, amount), 500);                setTimeout(() => triggerAutoCategorize(description, amount), 500);

            }            }

        });        });

    }    }

}}



async function triggerAutoCategorize(description, amount) {async function triggerAutoCategorize(description, amount) {

    const categoryNameField = document.getElementById('suggestedCategoryName');    const categoryNameField = document.getElementById('suggestedCategoryName');

    const categoryIdField = document.getElementById('suggestedCategoryId');    const categoryIdField = document.getElementById('suggestedCategoryId');

    const categoryHint = document.getElementById('categoryHint');    const categoryHint = document.getElementById('categoryHint');

    const categoryConfidence = document.getElementById('categoryConfidence');    const categoryConfidence = document.getElementById('categoryConfidence');

        

    if (!categoryNameField) return;    if (!categoryNameField) return;

        

    categoryNameField.value = 'Đang phân tích...';    categoryNameField.value = 'Đang phân tích...';

    categoryNameField.style.backgroundColor = '#fff3cd';    categoryNameField.style.backgroundColor = '#fff3cd';

        

    try {    try {

        const token = localStorage.getItem('authToken');        const token = localStorage.getItem('authToken');

        const response = await fetch('http://localhost:8080/api/ai/auto-categorize', {        const response = await fetch('http://localhost:8080/api/ai/auto-categorize', {

            method: 'POST',            method: 'POST',

            headers: {            headers: {

                'Content-Type': 'application/json',                'Content-Type': 'application/json',

                'Authorization': token ? `Bearer ${token}` : ''                'Authorization': token ? `Bearer ` : ''

            },            },

            body: JSON.stringify({            body: JSON.stringify({

                description: description,                description: description,

                amount: amount                amount: amount

            })            })

        });        });

                

        if (!response.ok) throw new Error('AI service error');        if (!response.ok) throw new Error('AI service error');

                

        const result = await response.json();        const result = await response.json();

                

        if (result.success && result.suggestions && result.suggestions.length > 0) {        if (result.success && result.suggestions && result.suggestions.length > 0) {

            const topSuggestion = result.suggestions[0];            const topSuggestion = result.suggestions[0];

                        

            categoryNameField.value = topSuggestion.categoryName || 'Không xác định';            categoryNameField.value = topSuggestion.categoryName || 'Không xác định';

            categoryIdField.value = topSuggestion.categoryId || '';            categoryIdField.value = topSuggestion.categoryId || '';

            categoryNameField.style.backgroundColor = '#d1e7dd';            categoryNameField.style.backgroundColor = '#d1e7dd';

                        

            if (categoryConfidence) categoryConfidence.style.display = 'flex';            if (categoryConfidence) categoryConfidence.style.display = 'flex';

                        

            if (categoryHint) {            if (categoryHint) {

                const confidence = topSuggestion.confidence || 0;                const confidence = topSuggestion.confidence || 0;

                categoryHint.innerHTML = `<i class="fas fa-check-circle text-success me-1"></i>Độ tin cậy: ${(confidence * 100).toFixed(0)}% - ${result.reasoning || ''}`;                categoryHint.innerHTML = `<i class=""fas fa-check-circle text-success me-1""></i>Độ tin cậy: % - `;

                categoryHint.classList.add('text-success');                categoryHint.classList.add('text-success');

            }            }

                        

            console.log('✓ Auto-categorized:', topSuggestion.categoryName, '(ID:', topSuggestion.categoryId, ')');            console.log('✓ Auto-categorized:', topSuggestion.categoryName, '(ID:', topSuggestion.categoryId, ')');

        } else {        } else {

            throw new Error('No suggestions');            throw new Error('No suggestions');

        }        }

    } catch (error) {    } catch (error) {

        console.error('Auto-categorize error:', error);        console.error('Auto-categorize error:', error);

        categoryNameField.value = 'Không thể phân loại';        categoryNameField.value = 'Không thể phân loại';

        categoryNameField.style.backgroundColor = '#f8d7da';        categoryNameField.style.backgroundColor = '#f8d7da';

        if (categoryHint) {        if (categoryHint) {

            categoryHint.innerHTML = '<i class="fas fa-exclamation-circle text-danger me-1"></i>Vui lòng thử lại';            categoryHint.innerHTML = '<i class=""fas fa-exclamation-circle text-danger me-1""></i>Vui lòng thử lại';

            categoryHint.classList.remove('text-success');            categoryHint.classList.remove('text-success');

            categoryHint.classList.add('text-danger');            categoryHint.classList.add('text-danger');

        }        }

    }    }

}}

