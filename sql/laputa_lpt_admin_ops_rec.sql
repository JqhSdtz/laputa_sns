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
-- Table structure for table `lpt_admin_ops_rec`
--

DROP TABLE IF EXISTS `lpt_admin_ops_rec`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lpt_admin_ops_rec` (
  `admin_ops_rec_id` int unsigned NOT NULL AUTO_INCREMENT,
  `admin_ops_rec_type` tinyint DEFAULT NULL,
  `admin_ops_rec_creator_id` int NOT NULL,
  `admin_ops_rec_target_id` int NOT NULL,
  `admin_ops_rec_desc` varchar(512) DEFAULT NULL,
  `admin_ops_rec_comment` varchar(256) DEFAULT NULL,
  `admin_ops_rec_create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`admin_ops_rec_id`),
  KEY `admin_ops_rec_creator_idx` (`admin_ops_rec_creator_id`),
  KEY `admin_ops_rec_target_idx` (`admin_ops_rec_target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lpt_admin_ops_rec`
--

LOCK TABLES `lpt_admin_ops_rec` WRITE;
/*!40000 ALTER TABLE `lpt_admin_ops_rec` DISABLE KEYS */;
INSERT INTO `lpt_admin_ops_rec` VALUES (2,7,2,0,'{\"id\":0,\"entity_type\":\"CATEGORY\",\"top_post_id\":259,\"is_leaf\":false,\"latest_cache_num\":0,\"popular_cache_num\":0}','测试管理员置顶功能','2020-04-26 14:19:03.000'),(3,13,2,28,'{\"user\":{\"type\":0,\"state\":0,\"nick_name\":\"测试号18\"},\"level\":3,\"category_id\":20,\"category_path\":[{\"id\":20,\"entity_type\":\"CATEGORY\",\"name\":\"test10\",\"latest_cache_num\":0,\"popular_cache_num\":0},{\"id\":0,\"entity_type\":\"CATEGORY\",\"name\":\"根目录\",\"latest_cache_num\":0,\"popular_cache_num\":0}]}','测试输入操作理由','2020-04-27 19:20:11.000'),(4,13,28,29,'{\"user\":{\"type\":0,\"state\":0,\"nick_name\":\"测试号19\"},\"level\":3,\"category_id\":20,\"category_path\":[{\"id\":20,\"entity_type\":\"CATEGORY\",\"name\":\"test10\"},{\"id\":0,\"entity_type\":\"CATEGORY\",\"name\":\"根目录\"}]}','测试管理员操作','2020-04-27 21:55:51.000'),(5,7,28,37,'{\"id\":37,\"entity_type\":\"CATEGORY\",\"top_post_id\":254}','置顶一个带图片的帖子','2020-05-05 13:01:34.000'),(6,1,2,13,'{\"id\":63,\"type\":0,\"ofId\":271,\"content\":\"tttteetttteeessstttesssttt\",\"creator\":{},\"entity_type\":\"CML1\",\"create_time\":1589089854000,\"l2_cnt\":0,\"like_cnt\":0,\"poster_rep_cnt\":0,\"parent_id\":271,\"creator_id\":13,\"post_id\":271}','不对不对不对','2020-05-10 13:53:51.000');
/*!40000 ALTER TABLE `lpt_admin_ops_rec` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-05-25 17:02:00
