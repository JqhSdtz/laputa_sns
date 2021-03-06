module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset'
  ],
  plugins: [
    ['import', {
      libraryName: 'ant-design-vue',
      libraryDirectory: 'es',
      style: true
      // style:  (name) => `${name}/style/index`
    }, 'ant-design-vue']
  ]
}
