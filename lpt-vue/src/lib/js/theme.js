export default {
    url: {
        default: 'https://lpt.ytumore.cn/static/lpt-vue/default'
    },
    colors: {
        default: [
            'rgb(255, 210, 210)',
            'rgb(210, 255, 210)',
            'rgb(210, 210, 255)'
        ]
    },
    getColor(index) {
        const arr = this.colors.default;
        return arr[index % arr.length];
    }
}