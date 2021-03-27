module.exports = {
    css: {
        loaderOptions: {
            less: {
                lessOptions: {
                    modifyVars: {
                        // 'primary-color': '#F0AAAA',
                        hack: `true; @import "./src/assets/css/variables.less";`,
                    },
                    javascriptEnabled: true
                }
            }
        }
    },
    devServer: {
        proxy: {
            '/api': {
                target: 'https://jqh.zone',
                secure: true,
                changeOrigin: true
            },
            '/static': {
                target: 'https://jqh.zone',
                secure: true,
                changeOrigin: true
            }
        }
    },
    pages: {
        lpt: {
            entry: './src/modules/lpt/lpt-main.js',
            template: 'public/lpt.html',
            title: 'laputa'
        },
        blog: {
            entry: './src/modules/blog/blog-main.js',
            template: 'public/blog.html',
            title: 'laputa-blog'
        }
    }
}