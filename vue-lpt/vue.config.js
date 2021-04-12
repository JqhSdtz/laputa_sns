//const BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;
const webpack = require('webpack');

module.exports = {
    configureWebpack: {
        plugins: [
            //new BundleAnalyzerPlugin(),
            new webpack.ContextReplacementPlugin(/moment[/\\]locale$/, /zh-cn/) //减小moment语言包体积
        ]
    },
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
        },
        blog: {
            entry: './src/modules/blog/blog-main.js',
            template: 'public/blog.html',
        }
    }
}