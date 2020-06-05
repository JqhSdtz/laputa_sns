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
-- Table structure for table `lpt_comment_l1`
--

DROP TABLE IF EXISTS `lpt_comment_l1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lpt_comment_l1` (
  `comment_l1_id` int unsigned NOT NULL AUTO_INCREMENT,
  `comment_l1_type` tinyint NOT NULL DEFAULT '0',
  `comment_l1_post_id` int NOT NULL DEFAULT '0',
  `comment_l1_content` varchar(256) DEFAULT NULL,
  `comment_l1_raw_img` varchar(256) DEFAULT NULL,
  `comment_l1_creator_id` int NOT NULL DEFAULT '0',
  `comment_l1_l2_cnt` int unsigned DEFAULT '0' COMMENT '包含的二级评论数量',
  `comment_l1_like_cnt` int unsigned DEFAULT '0',
  `comment_l1_poster_rep_cnt` int unsigned DEFAULT '0',
  `comment_l1_top_n_l2_id_str` varchar(55) DEFAULT NULL,
  `comment_l1_create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `comment_l1_p_indexed_flag` tinyint DEFAULT '0',
  `comment_l1_l_indexed_flag` tinyint DEFAULT '0',
  `comment_l1_del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`comment_l1_id`),
  KEY `comment_l1_post_id_idx` (`comment_l1_post_id`),
  KEY `comment_l1_creator_id_idx` (`comment_l1_creator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一级评论 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lpt_comment_l1`
--

LOCK TABLES `lpt_comment_l1` WRITE;
/*!40000 ALTER TABLE `lpt_comment_l1` DISABLE KEYS */;
INSERT INTO `lpt_comment_l1` VALUES (12,0,19,'test25',NULL,1,15,3,9,'13;11;27;','2020-03-01 19:14:51.000',0,0,0),(13,0,19,'test5',NULL,1,2,0,0,'9;10;','2020-03-01 19:14:57.000',0,0,0),(14,0,19,'test05',NULL,1,0,1,0,NULL,'2020-03-02 12:54:07.000',0,0,0),(15,0,19,'test05',NULL,1,0,3,0,NULL,'2020-03-02 12:55:21.000',0,0,0),(16,0,19,'test55',NULL,1,0,0,0,NULL,'2020-03-02 13:54:04.000',0,0,0),(17,0,15,'test55a',NULL,2,0,0,0,NULL,'2020-03-14 22:50:48.000',0,0,0),(18,0,19,'test5fd5a',NULL,2,0,0,0,NULL,'2020-03-22 14:54:54.000',0,0,0),(19,0,19,'test5fd5a',NULL,2,0,0,0,NULL,'2020-03-22 14:57:20.000',0,0,0),(20,0,19,'test5fd5a',NULL,2,0,0,0,NULL,'2020-03-22 15:10:41.000',0,0,0),(21,0,238,'测试评论！！',NULL,2,0,0,0,NULL,'2020-03-28 11:51:51.000',0,0,0),(22,0,238,'测试评论！！！',NULL,2,0,0,0,NULL,'2020-03-28 12:50:21.000',0,0,0),(23,0,238,'再次测试评论',NULL,2,0,1,0,NULL,'2020-03-28 12:52:29.000',0,0,0),(24,0,238,'再次再次测试评论',NULL,2,0,3,0,NULL,'2020-03-28 12:53:34.000',0,0,0),(25,0,238,'再再再次测试评论',NULL,2,0,1,0,NULL,'2020-03-28 12:56:50.000',0,0,0),(26,0,238,'我就不信了',NULL,2,0,2,0,NULL,'2020-03-28 13:00:20.000',0,0,0),(27,0,238,'应该可以了吧',NULL,2,6,1,1,'40;37;29;','2020-03-28 13:02:36.000',0,0,0),(28,0,239,'为什么会异常？',NULL,2,0,1,0,NULL,'2020-03-28 13:11:43.000',0,0,0),(29,0,248,'君不见黄河之水天上来',NULL,10,0,1,0,NULL,'2020-03-30 17:18:21.000',0,0,0),(30,0,248,'奔流到海不复回',NULL,10,2,1,2,'43;42;','2020-03-30 17:18:43.000',0,0,0),(31,0,248,'人生得意须尽欢',NULL,10,4,0,4,'44;54;46;','2020-03-30 17:27:07.000',0,0,0),(32,0,248,'烹羊宰牛且为乐',NULL,10,7,1,7,'48;47;53;','2020-03-30 17:32:30.000',0,0,0),(33,0,248,'陈王昔时宴平乐，斗酒十千恣欢谑',NULL,10,3,0,3,'57;56;55;','2020-03-30 17:39:05.000',0,0,0),(34,0,248,'海客谈瀛洲，烟涛微茫信难求',NULL,10,7,0,7,'62;64;63;','2020-03-30 17:53:25.000',0,0,0),(35,0,248,'测试再发再发再发',NULL,10,0,0,0,NULL,'2020-03-30 21:27:23.000',0,0,0),(36,0,239,'BUG无法复现是最郁闷的',NULL,10,0,0,0,NULL,'2020-03-30 21:36:22.000',0,0,0),(37,0,239,'半壁见海日，空中闻天鸡',NULL,10,0,0,0,NULL,'2020-03-30 21:36:55.000',0,0,0),(38,0,239,'千岩万转路不定，迷花倚石忽已暝',NULL,10,0,0,0,NULL,'2020-03-30 21:37:03.000',0,0,0),(39,0,239,'熊咆龙吟殷岩泉，栗深林兮惊层巅',NULL,10,0,1,0,NULL,'2020-03-30 21:37:17.000',0,0,0),(40,0,239,'云青青兮欲雨，水澹澹兮生烟',NULL,10,0,1,0,NULL,'2020-03-30 21:37:24.000',0,0,0),(41,0,239,'列缺霹雳，丘峦崩摧',NULL,10,0,1,0,NULL,'2020-03-30 21:37:33.000',0,0,0),(42,0,239,'洞天石扉，訇然中开',NULL,10,0,1,0,NULL,'2020-03-30 21:37:41.000',0,0,0),(43,0,247,'青冥浩荡不见底，日月照耀金银台',NULL,10,0,0,0,NULL,'2020-03-30 21:45:16.000',0,0,0),(44,0,246,'不对！！！',NULL,10,3,0,0,'67;66;65;','2020-03-30 21:48:11.000',0,0,0),(45,0,247,'霓为衣兮风为马，云之君兮纷纷而来下',NULL,10,0,2,0,NULL,'2020-03-30 21:50:18.000',0,0,0),(46,0,247,'虎鼓瑟兮鸾回车，仙之人兮列如麻',NULL,10,0,0,0,NULL,'2020-03-30 21:50:30.000',0,0,0),(47,0,247,'忽魂悸以魄动，恍惊起而长嗟',NULL,10,0,1,0,NULL,'2020-03-30 21:50:38.000',0,0,0),(48,0,247,'惟觉时之枕席，失向来之烟霞',NULL,10,0,1,0,NULL,'2020-03-30 21:50:45.000',0,0,0),(49,0,247,'世间行乐亦如此，古来万事东流水',NULL,10,0,0,0,NULL,'2020-03-30 21:51:12.000',0,0,0),(50,0,247,'别君去兮何时还？',NULL,10,0,0,0,NULL,'2020-03-30 21:51:28.000',0,0,0),(51,0,247,'测试回复',NULL,10,0,0,0,NULL,'2020-03-31 11:59:28.000',0,0,0),(52,0,242,'对还是不对。。。',NULL,10,0,0,0,NULL,'2020-03-31 12:03:05.000',0,0,0),(53,0,246,'前端好难，放弃了',NULL,10,0,0,0,NULL,'2020-04-01 14:19:53.000',0,0,0),(54,0,253,'可以可以',NULL,10,1,0,0,'68;','2020-04-07 16:36:07.000',0,0,0),(55,0,239,'第一届线上古诗词大赛',NULL,11,0,1,0,NULL,'2020-04-07 16:47:05.000',0,0,0),(56,0,259,'没问题',NULL,30,0,0,0,NULL,'2020-04-13 22:25:31.000',0,0,0),(57,0,260,'测试评论提醒',NULL,30,2,3,0,'69;70;','2020-04-20 16:00:36.000',0,0,0),(58,0,260,'再次测试评论提醒',NULL,30,0,3,0,NULL,'2020-04-20 16:00:46.000',0,0,0),(59,0,260,'testfornotice',NULL,30,0,3,0,NULL,'2020-04-20 16:09:15.000',0,0,0),(60,0,259,'测试counter',NULL,2,0,0,0,NULL,'2020-04-26 14:52:35.000',0,0,0),(61,0,268,'好诗！',NULL,2,0,0,0,NULL,'2020-05-04 15:14:22.000',0,0,0),(62,0,271,'testtesttesttesttesttest',NULL,10,2,0,0,'72;71;','2020-05-10 13:46:53.000',0,0,0),(63,0,271,'tttteetttteeessstttesssttt',NULL,13,0,0,0,NULL,'2020-05-10 13:50:54.000',0,0,1),(64,0,271,'似乎没有问题了',NULL,14,0,0,0,NULL,'2020-05-10 13:54:47.000',0,0,0),(65,0,265,'评论然后删除',NULL,2,0,0,0,NULL,'2020-05-10 13:59:21.000',0,0,0),(66,0,272,'我太菜了！',NULL,21,0,0,0,NULL,'2020-05-10 14:28:58.000',0,0,0),(67,0,261,'发表回复',NULL,31,0,0,0,NULL,'2020-05-11 16:31:55.000',0,0,0),(68,0,258,'新发表回复',NULL,31,0,0,0,NULL,'2020-05-11 16:39:19.000',0,0,0);
/*!40000 ALTER TABLE `lpt_comment_l1` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-05-25 17:01:59
