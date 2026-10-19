package uz.azizbek.maktabboshqaruv.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "room")
public  class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "building_id" , nullable = false)
    private Building building;

    @Column(nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Column
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column
    private RoomType type = RoomType.CLASSROOM;

    /** Nullable column (ddl-auto=update on a table with rows): null reads as ACTIVE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RoomStatus status;

    /** Equipment tags, comma separated: PROJECTOR, SMARTBOARD, AC (null = none). */
    @Column(length = 255)
    private String equipment;

    /** How many computers (a computer room); null = none. */
    @Column
    private Integer computers;

    public RoomStatus getStatus() {
        return status == null ? RoomStatus.ACTIVE : status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public java.util.List<String> equipmentTags() {
        if (equipment == null || equipment.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(equipment.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public Integer getComputers() {
        return computers;
    }

    public void setComputers(Integer computers) {
        this.computers = computers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }
    public void setBuilding(Building building){
        this.building = building;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }
}