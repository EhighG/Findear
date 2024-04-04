-- MySQL dump 10.13  Distrib 8.0.32, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: main
-- ------------------------------------------------------
-- Server version	8.0.32

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
-- Table structure for table `tbl_acquired_board`
--

DROP TABLE IF EXISTS `tbl_acquired_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_acquired_board` (
  `acquired_at` date DEFAULT NULL,
  `x_pos` float DEFAULT NULL,
  `y_pos` float DEFAULT NULL,
  `acquired_board_id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`acquired_board_id`),
  UNIQUE KEY `UK_lpklphtdsm3b4csx4uoym91lw` (`board_id`),
  CONSTRAINT `FKfeu1omgo2c61be5wxw7mwd6kv` FOREIGN KEY (`board_id`) REFERENCES `tbl_board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_acquired_board`
--

LOCK TABLES `tbl_acquired_board` WRITE;
/*!40000 ALTER TABLE `tbl_acquired_board` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_acquired_board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_agency`
--

DROP TABLE IF EXISTS `tbl_agency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_agency` (
  `x_pos` float DEFAULT NULL,
  `y_pos` float DEFAULT NULL,
  `agency_id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`agency_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_agency`
--

LOCK TABLES `tbl_agency` WRITE;
/*!40000 ALTER TABLE `tbl_agency` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_agency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_alarm`
--

DROP TABLE IF EXISTS `tbl_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_alarm` (
  `read_yn` bit(1) DEFAULT NULL,
  `alarm_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `generated_at` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`alarm_id`),
  KEY `FK7c38f8jri6mpnvs8yx1c88usc` (`member_id`),
  CONSTRAINT `FK7c38f8jri6mpnvs8yx1c88usc` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_alarm`
--

LOCK TABLES `tbl_alarm` WRITE;
/*!40000 ALTER TABLE `tbl_alarm` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_alarm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_board`
--

DROP TABLE IF EXISTS `tbl_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_board` (
  `delete_yn` bit(1) DEFAULT NULL,
  `is_lost` bit(1) DEFAULT NULL,
  `board_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `registered_at` datetime(6) DEFAULT NULL,
  `ai_description` text,
  `category_name` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `status` enum('ONGOING','DONE') DEFAULT NULL,
  PRIMARY KEY (`board_id`),
  KEY `FKcalhv1xx02f6jc09m8mhl298y` (`member_id`),
  CONSTRAINT `FKcalhv1xx02f6jc09m8mhl298y` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_board`
--

LOCK TABLES `tbl_board` WRITE;
/*!40000 ALTER TABLE `tbl_board` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_img_file`
--

DROP TABLE IF EXISTS `tbl_img_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_img_file` (
  `board_id` bigint DEFAULT NULL,
  `img_file_id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`img_file_id`),
  KEY `FK1fbfqshqj4upyie95ie9sc562` (`board_id`),
  CONSTRAINT `FK1fbfqshqj4upyie95ie9sc562` FOREIGN KEY (`board_id`) REFERENCES `tbl_board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_img_file`
--

LOCK TABLES `tbl_img_file` WRITE;
/*!40000 ALTER TABLE `tbl_img_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_img_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_lost112_scrap`
--

DROP TABLE IF EXISTS `tbl_lost112_scrap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_lost112_scrap` (
  `lost112_scrap_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `lost112_atc_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`lost112_scrap_id`),
  KEY `FK79cg5if6fkjahycos37g7m1kt` (`member_id`),
  CONSTRAINT `FK79cg5if6fkjahycos37g7m1kt` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_lost112_scrap`
--

LOCK TABLES `tbl_lost112_scrap` WRITE;
/*!40000 ALTER TABLE `tbl_lost112_scrap` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_lost112_scrap` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_lost_board`
--

DROP TABLE IF EXISTS `tbl_lost_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_lost_board` (
  `lost_at` date DEFAULT NULL,
  `x_pos` float DEFAULT NULL,
  `y_pos` float DEFAULT NULL,
  `board_id` bigint DEFAULT NULL,
  `lost_board_id` bigint NOT NULL AUTO_INCREMENT,
  `suspicious_place` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`lost_board_id`),
  UNIQUE KEY `UK_2bf048ns982m418isptwmmhjk` (`board_id`),
  CONSTRAINT `FKjoyr9rynu51d8vxy1w20fxtib` FOREIGN KEY (`board_id`) REFERENCES `tbl_board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_lost_board`
--

LOCK TABLES `tbl_lost_board` WRITE;
/*!40000 ALTER TABLE `tbl_lost_board` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_lost_board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_member`
--

DROP TABLE IF EXISTS `tbl_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_member` (
  `withdrawal_yn` bit(1) DEFAULT NULL,
  `agency_id` bigint DEFAULT NULL,
  `joined_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `withdrawal_at` datetime(6) DEFAULT NULL,
  `naver_refresh_token` varchar(255) DEFAULT NULL,
  `naver_uid` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) NOT NULL,
  `role` enum('NORMAL','MANAGER') NOT NULL,
  PRIMARY KEY (`member_id`),
  KEY `FKdiif279mhrcgn4ycxeey9hwtu` (`agency_id`),
  CONSTRAINT `FKdiif279mhrcgn4ycxeey9hwtu` FOREIGN KEY (`agency_id`) REFERENCES `tbl_agency` (`agency_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_member`
--

LOCK TABLES `tbl_member` WRITE;
/*!40000 ALTER TABLE `tbl_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_message`
--

DROP TABLE IF EXISTS `tbl_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `message_room_id` bigint DEFAULT NULL,
  `send_at` datetime(6) DEFAULT NULL,
  `sender_id` bigint DEFAULT NULL,
  `content` text,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `FK9dqr0k2l8juxkya2bjwl2aug8` (`message_room_id`),
  CONSTRAINT `FK9dqr0k2l8juxkya2bjwl2aug8` FOREIGN KEY (`message_room_id`) REFERENCES `tbl_message_room` (`message_room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_message`
--

LOCK TABLES `tbl_message` WRITE;
/*!40000 ALTER TABLE `tbl_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_message_room`
--

DROP TABLE IF EXISTS `tbl_message_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_message_room` (
  `board_id` bigint DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `message_room_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`message_room_id`),
  KEY `FKr5bm4dvell50qchufscaq61ol` (`board_id`),
  KEY `FKk5bqld01vj33qpuvyb61m5a94` (`member_id`),
  CONSTRAINT `FKk5bqld01vj33qpuvyb61m5a94` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`),
  CONSTRAINT `FKr5bm4dvell50qchufscaq61ol` FOREIGN KEY (`board_id`) REFERENCES `tbl_board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_message_room`
--

LOCK TABLES `tbl_message_room` WRITE;
/*!40000 ALTER TABLE `tbl_message_room` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_message_room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_notification`
--

DROP TABLE IF EXISTS `tbl_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_notification` (
  `member_id` bigint DEFAULT NULL,
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `token` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  UNIQUE KEY `UK_22h5h6kpansa3p4bfqar1rofc` (`member_id`),
  CONSTRAINT `FK5x6olmonauqildgrcngevvtle` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_notification`
--

LOCK TABLES `tbl_notification` WRITE;
/*!40000 ALTER TABLE `tbl_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_return_log`
--

DROP TABLE IF EXISTS `tbl_return_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_return_log` (
  `acquired_board_id` bigint DEFAULT NULL,
  `cancel_at` datetime(6) DEFAULT NULL,
  `return_log_id` bigint NOT NULL AUTO_INCREMENT,
  `returned_at` datetime(6) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`return_log_id`),
  KEY `FK4hobksgqioafa02e585f6xhj0` (`acquired_board_id`),
  CONSTRAINT `FK4hobksgqioafa02e585f6xhj0` FOREIGN KEY (`acquired_board_id`) REFERENCES `tbl_acquired_board` (`acquired_board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_return_log`
--

LOCK TABLES `tbl_return_log` WRITE;
/*!40000 ALTER TABLE `tbl_return_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_return_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tbl_scrap`
--

DROP TABLE IF EXISTS `tbl_scrap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_scrap` (
  `board_id` bigint DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `scrap_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`scrap_id`),
  KEY `FKb8m5y988jtqhn8xo8o3o7wxl3` (`board_id`),
  KEY `FKlj6ilqejlrw5dbx6pon4aeixq` (`member_id`),
  CONSTRAINT `FKb8m5y988jtqhn8xo8o3o7wxl3` FOREIGN KEY (`board_id`) REFERENCES `tbl_board` (`board_id`),
  CONSTRAINT `FKlj6ilqejlrw5dbx6pon4aeixq` FOREIGN KEY (`member_id`) REFERENCES `tbl_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tbl_scrap`
--

LOCK TABLES `tbl_scrap` WRITE;
/*!40000 ALTER TABLE `tbl_scrap` DISABLE KEYS */;
/*!40000 ALTER TABLE `tbl_scrap` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-04-03 22:52:03
