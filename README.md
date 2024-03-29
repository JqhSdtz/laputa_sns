# laputa_sns
一个移动端的社区网站以及一个PC端的博客网站。[社区网站实例](https://lpt.jqh.zone) [博客网站实例](https://jqh.zone)

后端使用Spring Boot，数据库使用MySQL，使用Redis构建缓存以及支持分布式（朝这个目标做的，但还没完全实现分布式）。

前端基于Vue3.0，混合了Ant-Design-Vue和Vant两套框架。

## 社区网站
支持发帖、评论、回复、转发、关注等功能。

有一个无限层级的目录机制，帖子在目录树的子节点分布，按照点赞数和发表时间逐级向上聚集，最终汇总到根目录。其中可以给某些目录设置为私有目录，帖子聚集到私有目录后不再继续向上。目录支持更改父目录，帖子支持更改目录。

所有内容列表均为无限下拉模式，不支持分页。

## 博客网站
支持首页、目录、相册等功能，组件基本复用自社区网站。

有一个悬浮球组件，网站功能菜单隐藏在悬浮球中。

有一个左侧抽屉，基于Ant-Design-Vue的抽屉组件，其包含一个路由视图，和主页面路由视图独立，其承载了博客很大一部分功能。

**感谢JetBrains对这个0 star项目的肯定！！**

[![JetBrains](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*ezhqu3*_ga*MTY3ODAyMTk1LjE2NDAwNjEwNzI.*_ga_V0XZL7QHEB*MTY0MDQyNzQ2My40LjAuMTY0MDQyNzQ3Mi4w&_ga=2.166412990.1081474564.1640347404-167802195.1640061072 "JetBrains")](https://www.jetbrains.com)
