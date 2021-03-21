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
            css: {
                loaderOptions: {
                    less: {
                        lessOptions: {
                            modifyVars: {
                                // 'primary-color': '#F0AAAA',
                                'button-primary-background-color': '#F0AAAA',
                                'overlay-background-color': 'rgba(255, 0, 0, 0.35)',
                                'action-sheet-header-height': '2rem'
                            },
                            javascriptEnabled: true
                        }
                    }
                }
            },
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