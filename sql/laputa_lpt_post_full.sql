-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: **.***.***.***    Database: laputa
-- ------------------------------------------------------
-- Server version	8.0.20

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `lpt_post_full`
--

DROP TABLE IF EXISTS `lpt_post_full`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lpt_post_full` (
  `post_full_id` int NOT NULL AUTO_INCREMENT,
  `post_full_text` text NOT NULL,
  PRIMARY KEY (`post_full_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='较长的帖子的完整内容 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lpt_post_full`
--

LOCK TABLES `lpt_post_full` WRITE;
/*!40000 ALTER TABLE `lpt_post_full` DISABLE KEYS */;
INSERT INTO `lpt_post_full` VALUES (1,'aaaaaaaaaaaaaaaaaa'),(2,'bbbbbbbbbbbbbbbb'),(3,'cccccccccccccccccccccccc'),(4,'沧海与满锦缎乎满浮萍却赏白云与守沧海且歌繁花者观沧海且赏蒹葭也满墨菊者泣月光乎泣苍穹与起锦缎之见白云与知兰芷与赏翠竹也歌沧海与见月光还起鸿雁却舞兰芷却歌苍穹还升蒹葭与观青山与赏鸿雁也寄鸿雁却知浮萍还观蒹葭乎立繁花且止鸿雁者赏腊梅还涌沧海且放墨菊与平苍穹且知秋叶者知柳岸且满沧海且流青山也平白云与采翠竹与守翠竹又采美酒乎起浮萍且起美酒者守月光之掩秋叶还寄腊梅者满美酒者平墨菊乎泣江水之起江水且采柳岸也歌秋叶乎拂蒹葭与歌沧海又平秋叶乎放腊梅还拂月光又流鸿雁也采青山也平沧海与升蒹葭之流美酒又起翠竹之寄繁花之掩鸿雁者寄江水乎观苍穹乎满墨菊者'),(5,'春江潮水连海平 海上明月共潮生\n滟滟随波千万里 何处春江无月明\n江流宛转绕芳甸 月照花林皆似霰\n空里流霜不觉飞 汀上白沙看不见\n江天一色无纤尘 皎皎空中孤月轮\n江畔何人初见月 江月何年初照人\n人生代代无穷已 江月年年望相似\n不知江月待何人 但见长江送流水\n白云一片去悠悠 青枫浦上不胜愁\n谁家今夜扁舟子 何处相思明月楼\n可怜楼上月徘徊 应照离人妆镜台\n玉户帘中卷不去 捣衣砧上拂还来\n此时相望不相闻 愿逐月华流照君\n鸿雁长飞光不度 鱼龙潜跃水成文\n昨夜闲潭梦落花 可怜春半不还家\n江水流春去欲尽 江潭落月复西斜\n斜月沉沉藏海雾 碣石潇湘无限路\n不知乘月几人归 落月摇情满江树'),(6,'海客谈瀛洲 烟涛微茫信难求\n越人语天姥 云霞明灭或可睹\n天姥连天向天横 势拔五岳掩赤城\n天台四万八千丈 对此欲倒东南倾\n\n我欲因之梦吴越 一夜飞度镜湖月\n湖月照我影 送我至剡溪\n谢公宿处今尚在 渌水荡漾清猿啼\n脚著谢公屐 身登青云梯\n半壁见海日 空中闻天鸡\n千岩万转路不定 迷花倚石忽已暝\n熊咆龙吟殷岩泉 栗深林兮惊层巅\n云青青兮欲雨 水澹澹兮生烟\n列缺霹雳 丘峦崩摧\n洞天石扉 訇然中开\n青冥浩荡不见底 日月照耀金银台\n霓为衣兮风为马 云之君兮纷纷而来下\n虎鼓瑟兮鸾回车 仙之人兮列如麻\n忽魂悸以魄动 恍惊起而长嗟\n惟觉时之枕席 失向来之烟霞\n\n世间行乐亦如此 古来万事东流水\n别君去兮何时还 且放白鹿青崖间 须行即骑访名山\n安能摧眉折腰事权贵 使我不得开心颜');
/*!40000 ALTER TABLE `lpt_post_full` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-05-25 17:02:05
