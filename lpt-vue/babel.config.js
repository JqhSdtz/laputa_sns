module.exports = {
    presets: [
        '@vue/cli-plugin-babel/preset'
    ],
    plugins: [
        ['import', {
            libraryName: 'ant-design-vue',
            libraryDirectory: 'es'
        }, 'ant-design-vue'],
        ['import', {
            libraryName: 'vant',
            libraryDirectory: 'es',
            style: true
        }, 'vant']
    ]
}
