import clamp from 'clamp-js';

const clampDir = {
    mounted(el, binding) {
        const rows = binding.value || 2;
        clamp(el, {clamp: rows});
    }
}

export default [
    {
        name: 'clamp',
        handler: clampDir
    }
];