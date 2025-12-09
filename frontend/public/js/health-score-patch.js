// Patch để fix health score rating tiếng Anh → tiếng Việt
console.log(" 🔧 health-score-patch.js loaded");

// Override updateHealthScoreDisplay function
const originalUpdateHealthScoreDisplay = window.updateHealthScoreDisplay;
window.updateHealthScoreDisplay = function(data) {
    console.log(" 🔧 updateHealthScoreDisplay intercepted - fixing rating translation");
    console.log(" 🔧 data:", data);
    
    const scoreValue = document.getElementById('healthScoreValue');
    const scoreProgress = document.getElementById('healthScoreProgress');
    const ratingBadge = document.getElementById('healthRatingBadge');
    const description = document.getElementById('healthDescription');
    
    console.log(" 🔧 Elements found:", { scoreValue: !!scoreValue, ratingBadge: !!ratingBadge, scoreProgress: !!scoreProgress });
    
    if (!scoreValue || !ratingBadge) {
        console.warn(" ⚠️ Health score elements not found");
        return;
    }
    
    // Update score value
    scoreValue.textContent = data.totalScore || 0;
    
    // Update progress circle
    if (scoreProgress) {
        const circumference = 339.292;
        const offset = circumference - (data.totalScore / 100) * circumference;
        scoreProgress.style.strokeDashoffset = offset;
    }
    
    // Update badge with TRANSLATION
    const ratingColors = {
        'EXCELLENT': 'success',
        'GOOD': 'info',
        'FAIR': 'warning',
        'POOR': 'danger',
        'CRITICAL': 'danger'
    };
    
    // FIX: Add Vietnamese translations
    const ratingTranslations = {
        'EXCELLENT': 'Xuất sắc',
        'GOOD': 'Tốt',
        'FAIR': 'Trung bình',
        'POOR': 'Kém',
        'CRITICAL': 'Tới hạn'
    };
    
    const badgeClass = ratingColors[data.rating] || 'secondary';
    const ratingText = ratingTranslations[data.rating] || data.rating || 'N/A';
    
    ratingBadge.className = `badge badge-lg badge-${badgeClass}`;
    ratingBadge.textContent = ratingText;
    
    console.log(" 🔧 Health score rating updated to Vietnamese:", { rating: data.rating, translated: ratingText });
    console.log(" 🔧 Badge element now shows:", ratingBadge.textContent);
    
    // Update description
    if (description && data.recommendations && data.recommendations.length > 0) {
        description.textContent = data.recommendations[0].message;
    }
};

// Also override any direct badge text updates
const observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
        if (mutation.target.id === 'healthRatingBadge' && mutation.type === 'characterData') {
            const badge = mutation.target;
            const enRating = badge.textContent.trim();
            const viRating = {
                'EXCELLENT': 'Xuất sắc',
                'GOOD': 'Tốt',
                'FAIR': 'Trung bình',
                'POOR': 'Kém',
                'CRITICAL': 'Tới hạn'
            }[enRating];
            
            if (viRating) {
                console.log(" 🔧 Intercepted direct badge update:", { from: enRating, to: viRating });
                badge.textContent = viRating;
            }
        }
    });
});

// Wait for DOM to be ready, then observe badge
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        const badge = document.getElementById('healthRatingBadge');
        if (badge) {
            observer.observe(badge, { characterData: true, subtree: true });
            console.log(" 🔧 Started observing healthRatingBadge");
        }
    });
} else {
    const badge = document.getElementById('healthRatingBadge');
    if (badge) {
        observer.observe(badge, { characterData: true, subtree: true });
        console.log(" 🔧 Started observing healthRatingBadge");
    }
}

console.log(" 🔧 health-score-patch.js: updateHealthScoreDisplay override applied");
