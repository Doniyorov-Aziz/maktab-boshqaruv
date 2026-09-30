package uz.azizbek.maktabboshqaruv.event;

/** Published by AnnouncementService when a new announcement is posted (edits do not re-notify). */
public record AnnouncementCreatedEvent(Long announcementId) {
}
