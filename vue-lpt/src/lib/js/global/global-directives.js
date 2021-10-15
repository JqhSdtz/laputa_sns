import clamp from 'clamp-js';

const clampDir = {
    mounted(el, binding) {
        const rows = binding.value || 2;
        clamp(el, {clamp: rows});
    }
}

const draggable = {
    mounted(el, binding) {
        let isDragging = false;
        let hasMoved = false;
        let lastClickTime = new Date().getTime();
        const offset = {};
        const param = binding.value;
        const startDrag = (event) => {
            isDragging = true;
            hasMoved = false;
            const curX = event.type === 'touchstart' ? event.touches[0].clientX : event.clientX;
            const curY = event.type === 'touchstart' ? event.touches[0].clientY : event.clientY;
            offset.x = el.offsetLeft - curX;
            offset.y = el.offsetTop - curY;
            param && param.start && param.start();
        };
        const endDrag = () => {
            if (isDragging && !hasMoved) {
                const curTime = new Date().getTime();
                if (curTime - lastClickTime > 100) {
                    // 触摸事件可能和点击事件连续触发，判断间隔时间大于100毫秒才算正常点击
                    param && param.click && param.click();
                    lastClickTime = curTime;
                }
            }
            isDragging = false;
            hasMoved = false;
            param && param.end && param.end();
        };
        const dragging = (event) => {
            //event.preventDefault();
            if (isDragging) {
                hasMoved = true;
                const curX = event.type === 'touchmove' ? event.touches[0].clientX : event.clientX;
                const curY = event.type === 'touchmove' ? event.touches[0].clientY : event.clientY;
                const x = offset.x + curX;
                const y = offset.y + curY;
                el.style.left = x + 'px';
                el.style.top = y + 'px';
                param && param.move && param.move({x, y});
            }
        }
        el.addEventListener('mousedown', startDrag, true);
        el.addEventListener('touchstart', startDrag, true);
        el.addEventListener('mouseup', endDrag, true);
        el.addEventListener('touchend', endDrag, true);
        // 如果用el监听移动事件，移动过快时鼠标移动到元素外还没来得及更改位置，就会停止移动
        // 所以要监听全局的移动事件
        window.document.addEventListener('mousemove', dragging, true);
        window.document.addEventListener('touchmove', dragging, {passive: false});
        let preWinWidth = document.body.clientWidth;
        let preWinHeight = document.body.clientHeight;
        window.addEventListener('resize', () => {
            // 保证窗口大小改变的时候，元素的相对位置不变，不会跑到可视范围外
            const preX = parseFloat(el.style.left);
            const preY = parseFloat(el.style.top);
            const curWinWidth = document.body.clientWidth;
            const curWinHeight = document.body.clientHeight;
            el.style.left = curWinWidth / preWinWidth * preX + 'px';
            el.style.top = curWinHeight / preWinHeight * preY + 'px';
            preWinWidth = curWinWidth;
            preWinHeight = curWinHeight;
        });
    }
}

const scrollView = {
    mounted(el, binding, vNode) {
        let hasScrolledTop = false;
        if (vNode.dirs[0].instance.lptContainer === 'blogMain') {
            el.onwheel = (event) => {
                if (event.deltaY < 0 && window.scrollY !== 0) {
                    // 向上滚动，且页面滚动条不在顶端，则内容不滚动，整个页面滚动
                    if (!hasScrolledTop) {
                        window.scroll({
                            top: 0,
                            left: 0,
                            behavior: 'smooth'
                        });
                    }
                    hasScrolledTop = true;
                    event.preventDefault();
                } else if (event.deltaY > 0) {
                    hasScrolledTop = window.scrollY === 0;
                }
            };
            window.addEventListener('wheel', (event) => {
                if (!el.contains(event.target) && (window.scrollY !== 0 || event.deltaY > 0)) {
                    hasScrolledTop = false;
                }
            });
        }
    }
}

export default [
    {
        name: 'clamp',
        handler: clampDir
    },
    {
        name: 'draggable',
        handler: draggable
    },
    {
        name: 'scrollView',
        handler: scrollView
    }
];