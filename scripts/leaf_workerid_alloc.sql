DROP TABLE IF EXISTS `leaf_workerid_alloc`;

CREATE TABLE `leaf_workerid_alloc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(127) DEFAULT NULL,
  `port` varchar(127) DEFAULT NULL,
  `ip_port` varchar(127) NOT NULL,
  `max_timestamp` bigint(20) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_ipport` (`ip_port`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=0