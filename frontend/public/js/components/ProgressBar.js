// Progress Bar Component
import { COLORS } from '../constants/colors.js';

class ProgressBar {
  constructor(container, options = {}) {
    this.container = container;
    this.options = {
      type: 'linear', // linear, circular, mini
      size: 'medium', // small, medium, large
      showLabel: true,
      showPercentage: true,
      showValue: false,
      animated: true,
      color: 'auto', // auto, success, warning, danger, info
      gradient: false,
      striped: false,
      ...options
    };
    
    this.value = 0;
    this.maxValue = 100;
    this.label = '';
    this.status = 'normal'; // normal, success, warning, danger
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
  }

  render() {
    if (this.options.type === 'circular') {
      this.renderCircular();
    } else if (this.options.type === 'mini') {
      this.renderMini();
    } else {
      this.renderLinear();
    }
    
    this.renderStyles();
  }

  renderLinear() {
    const percentage = this.calculatePercentage();
    const colorClass = this.getColorClass();
    
    this.container.innerHTML = `
      <div class="progress-bar-container linear ${this.options.size} ${colorClass}">
        ${this.options.showLabel ? `
          <div class="progress-header">
            <span class="progress-label">${this.label}</span>
            ${this.renderValueDisplay(percentage)}
          </div>
        ` : ''}
        
        <div class="progress-track ${this.options.striped ? 'striped' : ''}">
          <div class="progress-fill ${this.options.animated ? 'animated' : ''} ${this.options.gradient ? 'gradient' : ''}" 
               style="width: ${percentage}%"
               data-percentage="${percentage.toFixed(1)}">
            ${this.options.showPercentage && this.options.size !== 'small' ? `
              <span class="progress-percentage">${percentage.toFixed(0)}%</span>
            ` : ''}
          </div>
        </div>

        ${this.renderProgressIndicators()}
      </div>
    `;
  }

  renderCircular() {
    const percentage = this.calculatePercentage();
    const colorClass = this.getColorClass();
    const circumference = 2 * Math.PI * 45; // radius = 45
    const strokeDasharray = circumference;
    const strokeDashoffset = circumference - (percentage / 100) * circumference;
    
    this.container.innerHTML = `
      <div class="progress-bar-container circular ${this.options.size} ${colorClass}">
        <div class="circular-progress">
          <svg width="100" height="100" viewBox="0 0 100 100">
            <circle class="progress-track-circle" 
                    cx="50" cy="50" r="45" 
                    stroke-width="6" 
                    fill="none"/>
            <circle class="progress-fill-circle ${this.options.animated ? 'animated' : ''}" 
                    cx="50" cy="50" r="45" 
                    stroke-width="6" 
                    fill="none"
                    stroke-dasharray="${strokeDasharray}"
                    stroke-dashoffset="${strokeDashoffset}"/>
          </svg>
          
          <div class="circular-content">
            ${this.options.showPercentage ? `
              <span class="circular-percentage">${percentage.toFixed(0)}%</span>
            ` : ''}
            ${this.options.showLabel ? `
              <span class="circular-label">${this.label}</span>
            ` : ''}
          </div>
        </div>

        ${this.options.showValue ? this.renderValueDisplay(percentage) : ''}
      </div>
    `;
  }

  renderMini() {
    const percentage = this.calculatePercentage();
    const colorClass = this.getColorClass();
    
    this.container.innerHTML = `
      <div class="progress-bar-container mini ${colorClass}">
        <div class="mini-progress">
          <div class="progress-track">
            <div class="progress-fill ${this.options.animated ? 'animated' : ''}" 
                 style="width: ${percentage}%"></div>
          </div>
          
          ${this.options.showPercentage || this.options.showLabel ? `
            <div class="mini-info">
              ${this.options.showLabel ? `<span class="mini-label">${this.label}</span>` : ''}
              ${this.options.showPercentage ? `<span class="mini-percentage">${percentage.toFixed(0)}%</span>` : ''}
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }

  renderValueDisplay(percentage) {
    let display = '';
    
    if (this.options.showPercentage) {
      display += `<span class="value-percentage">${percentage.toFixed(1)}%</span>`;
    }
    
    if (this.options.showValue) {
      display += `<span class="value-numbers">${this.formatValue(this.value)}/${this.formatValue(this.maxValue)}</span>`;
    }
    
    return display ? `<div class="value-display">${display}</div>` : '';
  }

  renderProgressIndicators() {
    if (!this.options.showIndicators) return '';
    
    const milestones = this.calculateMilestones();
    if (milestones.length === 0) return '';

    return `
      <div class="progress-indicators">
        ${milestones.map(milestone => `
          <div class="indicator ${milestone.reached ? 'reached' : ''}" 
               style="left: ${milestone.percentage}%">
            <div class="indicator-line"></div>
            <span class="indicator-label">${milestone.label}</span>
          </div>
        `).join('')}
      </div>
    `;
  }

  calculatePercentage() {
    if (this.maxValue === 0) return 0;
    return Math.min((this.value / this.maxValue) * 100, 100);
  }

  getColorClass() {
    if (this.options.color !== 'auto') {
      return `color-${this.options.color}`;
    }
    
    const percentage = this.calculatePercentage();
    
    if (this.status === 'danger' || percentage > 100) return 'color-danger';
    if (this.status === 'warning' || percentage > 80) return 'color-warning';
    if (this.status === 'success' || percentage === 100) return 'color-success';
    
    return 'color-primary';
  }

  calculateMilestones() {
    if (!this.milestones) return [];
    
    return this.milestones.map(milestone => ({
      ...milestone,
      percentage: (milestone.value / this.maxValue) * 100,
      reached: this.value >= milestone.value
    }));
  }

  formatValue(value) {
    if (typeof value === 'number') {
      if (value >= 1000000) {
        return `${(value / 1000000).toFixed(1)}M`;
      } else if (value >= 1000) {
        return `${(value / 1000).toFixed(1)}K`;
      }
      return value.toFixed(0);
    }
    return value;
  }

  attachEventListeners() {
    // Add hover effects for interactive elements
    if (this.options.interactive) {
      this.container.addEventListener('click', () => {
        this.onClick?.();
      });

      this.container.addEventListener('mouseenter', () => {
        this.container.classList.add('hovered');
      });

      this.container.addEventListener('mouseleave', () => {
        this.container.classList.remove('hovered');
      });
    }
  }

  // Public methods
  setValue(value, maxValue = null) {
    this.value = value || 0;
    if (maxValue !== null) {
      this.maxValue = maxValue;
    }
    this.render();
  }

  setLabel(label) {
    this.label = label || '';
    this.render();
  }

  setStatus(status) {
    this.status = status;
    this.render();
  }

  setMilestones(milestones) {
    this.milestones = milestones;
    this.options.showIndicators = true;
    this.render();
  }

  animate(targetValue, duration = 1000) {
    const startValue = this.value;
    const startTime = Date.now();
    
    const animateFrame = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Easing function (ease-out)
      const easedProgress = 1 - Math.pow(1 - progress, 3);
      
      this.value = startValue + (targetValue - startValue) * easedProgress;
      this.render();
      
      if (progress < 1) {
        requestAnimationFrame(animateFrame);
      } else {
        this.value = targetValue;
        this.render();
      }
    };
    
    requestAnimationFrame(animateFrame);
  }

  reset() {
    this.value = 0;
    this.render();
  }

  refresh() {
    this.render();
  }

  renderStyles() {
    if (document.getElementById('progress-bar-styles')) return;

    const style = document.createElement('style');
    style.id = 'progress-bar-styles';
    style.textContent = `
      .progress-bar-container {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      /* Linear Progress Bar */
      .progress-bar-container.linear {
        width: 100%;
      }

      .progress-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.5rem;
      }

      .progress-label {
        font-size: 0.875rem;
        font-weight: 500;
        color: #374151;
      }

      .value-display {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.875rem;
        color: #6b7280;
      }

      .value-percentage {
        font-weight: 600;
        color: #111827;
      }

      .progress-track {
        position: relative;
        height: 8px;
        background: #f3f4f6;
        border-radius: 4px;
        overflow: hidden;
      }

      .progress-bar-container.small .progress-track {
        height: 4px;
      }

      .progress-bar-container.large .progress-track {
        height: 12px;
        border-radius: 6px;
      }

      .progress-fill {
        height: 100%;
        border-radius: inherit;
        transition: width 0.3s ease;
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .progress-fill.animated {
        transition: width 0.6s ease-out;
      }

      .progress-fill.gradient {
        background: linear-gradient(90deg, var(--progress-color-start), var(--progress-color-end)) !important;
      }

      .progress-track.striped .progress-fill {
        background-image: linear-gradient(
          45deg,
          rgba(255, 255, 255, 0.15) 25%,
          transparent 25%,
          transparent 50%,
          rgba(255, 255, 255, 0.15) 50%,
          rgba(255, 255, 255, 0.15) 75%,
          transparent 75%,
          transparent
        );
        background-size: 1rem 1rem;
        animation: progress-stripes 1s linear infinite;
      }

      @keyframes progress-stripes {
        0% { background-position: 0 0; }
        100% { background-position: 1rem 0; }
      }

      .progress-percentage {
        font-size: 0.75rem;
        font-weight: 600;
        color: white;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
      }

      .progress-bar-container.small .progress-percentage {
        font-size: 0.625rem;
      }

      /* Circular Progress Bar */
      .progress-bar-container.circular {
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .circular-progress {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .progress-bar-container.circular.small svg {
        width: 60px;
        height: 60px;
      }

      .progress-bar-container.circular.large svg {
        width: 120px;
        height: 120px;
      }

      .progress-track-circle {
        stroke: #f3f4f6;
      }

      .progress-fill-circle {
        stroke: var(--progress-color);
        stroke-linecap: round;
        transform: rotate(-90deg);
        transform-origin: 50% 50%;
        transition: stroke-dashoffset 0.3s ease;
      }

      .progress-fill-circle.animated {
        transition: stroke-dashoffset 0.6s ease-out;
      }

      .circular-content {
        position: absolute;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
      }

      .circular-percentage {
        font-size: 1.25rem;
        font-weight: 700;
        color: #111827;
      }

      .progress-bar-container.circular.small .circular-percentage {
        font-size: 0.875rem;
      }

      .progress-bar-container.circular.large .circular-percentage {
        font-size: 1.75rem;
      }

      .circular-label {
        font-size: 0.75rem;
        color: #6b7280;
        text-align: center;
        margin-top: 0.125rem;
      }

      /* Mini Progress Bar */
      .progress-bar-container.mini {
        width: 100%;
      }

      .mini-progress {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .mini-progress .progress-track {
        flex: 1;
        height: 4px;
        background: #f3f4f6;
        border-radius: 2px;
      }

      .mini-info {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-size: 0.75rem;
        white-space: nowrap;
      }

      .mini-label {
        color: #6b7280;
      }

      .mini-percentage {
        font-weight: 600;
        color: #374151;
      }

      /* Progress Indicators */
      .progress-indicators {
        position: relative;
        margin-top: 0.5rem;
        height: 20px;
      }

      .indicator {
        position: absolute;
        transform: translateX(-50%);
      }

      .indicator-line {
        width: 2px;
        height: 8px;
        background: #d1d5db;
        margin: 0 auto 0.25rem;
      }

      .indicator.reached .indicator-line {
        background: var(--progress-color);
      }

      .indicator-label {
        font-size: 0.625rem;
        color: #6b7280;
        display: block;
        text-align: center;
        white-space: nowrap;
      }

      .indicator.reached .indicator-label {
        color: #374151;
        font-weight: 500;
      }

      /* Color Variations */
      .color-primary {
        --progress-color: #3b82f6;
        --progress-color-start: #3b82f6;
        --progress-color-end: #1d4ed8;
      }

      .color-primary .progress-fill {
        background: var(--progress-color);
      }

      .color-success {
        --progress-color: #10b981;
        --progress-color-start: #10b981;
        --progress-color-end: #059669;
      }

      .color-success .progress-fill {
        background: var(--progress-color);
      }

      .color-warning {
        --progress-color: #f59e0b;
        --progress-color-start: #f59e0b;
        --progress-color-end: #d97706;
      }

      .color-warning .progress-fill {
        background: var(--progress-color);
      }

      .color-danger {
        --progress-color: #ef4444;
        --progress-color-start: #ef4444;
        --progress-color-end: #dc2626;
      }

      .color-danger .progress-fill {
        background: var(--progress-color);
      }

      .color-info {
        --progress-color: #06b6d4;
        --progress-color-start: #06b6d4;
        --progress-color-end: #0891b2;
      }

      .color-info .progress-fill {
        background: var(--progress-color);
      }

      /* Interactive States */
      .progress-bar-container.interactive {
        cursor: pointer;
        transition: transform 0.2s ease;
      }

      .progress-bar-container.interactive:hover {
        transform: scale(1.02);
      }

      .progress-bar-container.interactive.hovered .progress-fill {
        filter: brightness(1.1);
      }

      /* Accessibility */
      .progress-bar-container[role="progressbar"] {
        outline: none;
      }

      .progress-bar-container[role="progressbar"]:focus-visible {
        outline: 2px solid #3b82f6;
        outline-offset: 2px;
        border-radius: 4px;
      }

      /* Responsive Design */
      @media (max-width: 768px) {
        .progress-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 0.25rem;
          margin-bottom: 0.75rem;
        }

        .value-display {
          font-size: 0.75rem;
        }

        .circular-percentage {
          font-size: 1rem;
        }

        .progress-bar-container.circular.large .circular-percentage {
          font-size: 1.25rem;
        }

        .mini-info {
          flex-direction: column;
          align-items: flex-end;
          gap: 0.125rem;
        }

        .indicator-label {
          font-size: 0.5rem;
        }
      }

      @media (max-width: 480px) {
        .progress-bar-container.large .progress-track {
          height: 8px;
        }

        .progress-bar-container.circular.large svg {
          width: 100px;
          height: 100px;
        }

        .progress-indicators {
          height: 16px;
        }

        .indicator-line {
          height: 6px;
        }
      }

      /* Dark Mode Support */
      @media (prefers-color-scheme: dark) {
        .progress-label {
          color: #d1d5db;
        }

        .value-percentage {
          color: #f3f4f6;
        }

        .value-display {
          color: #9ca3af;
        }

        .progress-track {
          background: #374151;
        }

        .progress-track-circle {
          stroke: #374151;
        }

        .circular-percentage {
          color: #f3f4f6;
        }

        .circular-label {
          color: #9ca3af;
        }

        .mini-label {
          color: #9ca3af;
        }

        .mini-percentage {
          color: #d1d5db;
        }

        .indicator-line {
          background: #6b7280;
        }

        .indicator-label {
          color: #9ca3af;
        }

        .indicator.reached .indicator-label {
          color: #d1d5db;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default ProgressBar;