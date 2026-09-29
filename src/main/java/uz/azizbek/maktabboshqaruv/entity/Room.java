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