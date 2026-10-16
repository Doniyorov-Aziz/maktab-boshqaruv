package uz.azizbek.maktabboshqaruv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduled jobs (attendance automation and demo, Telegram outbox, library digests) — on in every profile. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
