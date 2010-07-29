SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `wikipedia_clean` DEFAULT CHARACTER SET utf8;

-- -----------------------------------------------------
-- Table `wikipedia`.`pages_nl`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `wikipedia_clean`.`pages_nl` (
  `id` INT UNSIGNED NOT NULL ,
  `title` TEXT NOT NULL ,
  `body` MEDIUMTEXT NOT NULL ,
  `en_link` TEXT NULL ,
  `entity_type` VARCHAR(10) NULL ,
  `dp` TINYINT(1)  NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) )
ENGINE = XtraDB;


-- -----------------------------------------------------
-- Table `wikipedia`.`pages_en`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `wikipedia_clean`.`pages_en` (
  `id` INT UNSIGNED NOT NULL ,
  `title` TEXT NOT NULL ,
  `body` MEDIUMTEXT NOT NULL ,
  `en_link` TEXT NULL ,
  `entity_type` VARCHAR(10) NULL ,
  `dp` TINYINT(1)  NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) )
ENGINE = XtraDB;


-- -----------------------------------------------------
-- Table `wikipedia`.`cats_nl`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `wikipedia_clean`.`cats_nl` (
  `page_id` INT UNSIGNED NOT NULL ,
  `cat_name` VARCHAR(225) NOT NULL ,
  PRIMARY KEY (`page_id`, `cat_name`) )
ENGINE = XtraDB;


-- -----------------------------------------------------
-- Table `wikipedia`.`cats_en`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `wikipedia_clean`.`cats_en` (
  `page_id` INT UNSIGNED NOT NULL ,
  `cat_name` VARCHAR(225) NOT NULL ,
  PRIMARY KEY (`cat_name`, `page_id`) )
ENGINE = XtraDB;


-- -----------------------------------------------------
-- Table `wikipedia`.`dictionary`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `wikipedia_clean`.`dictionary` (
  `id` INT UNSIGNED NOT NULL ,
  `name` VARCHAR(50) NOT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) )
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
