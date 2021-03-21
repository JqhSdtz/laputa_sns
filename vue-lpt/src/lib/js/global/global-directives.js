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
        const offset = {};
        const param = binding.value;
        const startDrag = (event) => {
            isDragging = true;
            offset.x = el.offsetLeft - event.clientX;
            offset.y = el.offsetTop - event.clientY;
            param && param.start && param.start();
        };
        const endDrag = () => {
            if (isDragging && !hasMoved) {
                param && param.click && param.click();
            }
            isDragging = false;
            hasMoved = false;
            param && param.end && param.end();
        };
        const dragging = (event) => {
            event.preventDefault();
            if (isDragging) {
                hasMoved = true;
                const x = offset.x + event.clientX;
                const y = offset.y + event.clientY;
                el.style.left = x + 'px';
                el.style.top  = y + 'px';
                param && param.move && param.move({x, y});
            }
        }
        el.addEventListener('mousedown', startDrag, true);
        el.addEventListener('mouseup', endDrag, true);
        // 如果用el监听移动事件，移动过快时鼠标移动到元素外还没来得及更改位置，就会停止移动
        // 所以要监听全局的移动事件
        window.document.addEventListener('mousemove', dragging, true);
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
    }
];