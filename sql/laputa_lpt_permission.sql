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
-- Table structure for table `lpt_permission`
--

DROP TABLE IF EXISTS `lpt_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lpt_permission` (
  `permission_id` int unsigned NOT NULL AUTO_INCREMENT,
  `permission_user_id` int NOT NULL,
  `permission_category_id` int NOT NULL,
  `permission_level` tinyint DEFAULT NULL,
  `permission_creator_id` int NOT NULL,
  `permission_creat_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `permission_unique_idx` (`permission_user_id`,`permission_category_id`) COMMENT '根据user_id查category_id更加频繁，所以把user_id放在第一项',
  KEY `permission_creator_idx` (`permission_creator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lpt_permission`
--

LOCK TABLES `lpt_permission` WRITE;
/*!40000 ALTER TABLE `lpt_permission` DISABLE KEYS */;
INSERT INTO `lpt_permission` VALUES (10,2,0,99,1,'2020-02-14 19:52:27.000'),(17,28,0,3,2,'2020-04-27 19:02:34.000'),(18,28,20,3,2,'2020-04-27 19:20:11.000'),(23,29,20,3,28,'2020-04-27 21:55:51.000');
/*!40000 ALTER TABLE `lpt_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-05-25 17:02:03
