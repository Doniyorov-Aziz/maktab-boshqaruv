package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.District;
import uz.azizbek.maktabboshqaruv.entity.Region;
import uz.azizbek.maktabboshqaruv.repository.DistrictRepository;
import uz.azizbek.maktabboshqaruv.repository.RegionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hududlar va tumanlar ma'lumotnomasi, in every environment. A region is added only when its SOATO
 * code is missing (the live database got its 14 by hand); a region's districts only when it has none
 * yet — so names an admin edited or deleted are never brought back. District SOATO codes are left
 * empty: fill them from the official classifier (stat.uz) in the "Tumanlar" list.
 */
@Component
@Order(1)
public class TerritorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TerritorySeeder.class);

    record RegionSeed(String code, String name, List<String> districts) {
    }

    static final List<RegionSeed> REGIONS = List.of(
            new RegionSeed("1735", "Qoraqalpog'iston Respublikasi", List.of("Nukus shahri", "Amudaryo tumani",
                    "Beruniy tumani", "Bo'zatov tumani", "Chimboy tumani", "Ellikqal'a tumani", "Kegeyli tumani",
                    "Mo'ynoq tumani", "Nukus tumani", "Qanliko'l tumani", "Qo'ng'irot tumani", "Qorao'zak tumani",
                    "Shumanay tumani", "Taxtako'pir tumani", "To'rtko'l tumani", "Xo'jayli tumani")),
            new RegionSeed("1703", "Andijon viloyati", List.of("Andijon shahri", "Xonobod shahri", "Andijon tumani",
                    "Asaka tumani", "Baliqchi tumani", "Bo'ston tumani", "Buloqboshi tumani", "Izboskan tumani",
                    "Jalaquduq tumani", "Xo'jaobod tumani", "Qo'rg'ontepa tumani", "Marhamat tumani",
                    "Oltinko'l tumani", "Paxtaobod tumani", "Shahrixon tumani", "Ulug'nor tumani")),
            new RegionSeed("1706", "Buxoro viloyati", List.of("Buxoro shahri", "Kogon shahri", "Buxoro tumani",
                    "G'ijduvon tumani", "Jondor tumani", "Kogon tumani", "Olot tumani", "Peshku tumani",
                    "Qorako'l tumani", "Qorovulbozor tumani", "Romitan tumani", "Shofirkon tumani", "Vobkent tumani")),
            new RegionSeed("1708", "Jizzax viloyati", List.of("Jizzax shahri", "Arnasoy tumani", "Baxmal tumani",
                    "Do'stlik tumani", "Forish tumani", "G'allaorol tumani", "Sharof Rashidov tumani",
                    "Mirzacho'l tumani", "Paxtakor tumani", "Yangiobod tumani", "Zafarobod tumani", "Zarbdor tumani",
                    "Zomin tumani")),
            new RegionSeed("1710", "Qashqadaryo viloyati", List.of("Qarshi shahri", "Shahrisabz shahri",
                    "Chiroqchi tumani", "Dehqonobod tumani", "G'uzor tumani", "Kasbi tumani", "Kitob tumani",
                    "Koson tumani", "Ko'kdala tumani", "Mirishkor tumani", "Muborak tumani", "Nishon tumani",
                    "Qamashi tumani", "Qarshi tumani", "Shahrisabz tumani", "Yakkabog' tumani")),
            new RegionSeed("1712", "Navoiy viloyati", List.of("Navoiy shahri", "Zarafshon shahri", "Karmana tumani",
                    "Konimex tumani", "Navbahor tumani", "Nurota tumani", "Qiziltepa tumani", "Tomdi tumani",
                    "Uchquduq tumani", "Xatirchi tumani")),
            new RegionSeed("1714", "Namangan viloyati", List.of("Namangan shahri", "Chortoq tumani", "Chust tumani",
                    "Kosonsoy tumani", "Mingbuloq tumani", "Namangan tumani", "Norin tumani", "Pop tumani",
                    "To'raqo'rg'on tumani", "Uchqo'rg'on tumani", "Uychi tumani", "Yangiqo'rg'on tumani")),
            new RegionSeed("1718", "Samarqand viloyati", List.of("Samarqand shahri", "Kattaqo'rg'on shahri",
                    "Bulung'ur tumani", "Ishtixon tumani", "Jomboy tumani", "Kattaqo'rg'on tumani", "Narpay tumani",
                    "Nurobod tumani", "Oqdaryo tumani", "Paxtachi tumani", "Pastdarg'om tumani", "Payariq tumani",
                    "Qo'shrabot tumani", "Samarqand tumani", "Tayloq tumani", "Urgut tumani")),
            new RegionSeed("1722", "Surxondaryo viloyati", List.of("Termiz shahri", "Angor tumani", "Bandixon tumani",
                    "Boysun tumani", "Denov tumani", "Jarqo'rg'on tumani", "Muzrabot tumani", "Oltinsoy tumani",
                    "Qiziriq tumani", "Qumqo'rg'on tumani", "Sariosiyo tumani", "Sherobod tumani", "Sho'rchi tumani",
                    "Termiz tumani", "Uzun tumani")),
            new RegionSeed("1724", "Sirdaryo viloyati", List.of("Guliston shahri", "Yangiyer shahri", "Shirin shahri",
                    "Boyovut tumani", "Guliston tumani", "Mirzaobod tumani", "Oqoltin tumani", "Sardoba tumani",
                    "Sayxunobod tumani", "Sirdaryo tumani", "Xovos tumani")),
            new RegionSeed("1727", "Toshkent viloyati", List.of("Nurafshon shahri", "Angren shahri", "Bekobod shahri",
                    "Chirchiq shahri", "Olmaliq shahri", "Ohangaron shahri", "Yangiyo'l shahri", "Bekobod tumani",
                    "Bo'ka tumani", "Bo'stonliq tumani", "Chinoz tumani", "Qibray tumani", "Ohangaron tumani",
                    "Oqqo'rg'on tumani", "Parkent tumani", "Piskent tumani", "Quyichirchiq tumani",
                    "O'rtachirchiq tumani", "Yangiyo'l tumani", "Yuqorichirchiq tumani", "Zangiota tumani",
                    "Toshkent tumani")),
            new RegionSeed("1730", "Farg'ona viloyati", List.of("Farg'ona shahri", "Marg'ilon shahri", "Qo'qon shahri",
                    "Quvasoy shahri", "Beshariq tumani", "Bog'dod tumani", "Buvayda tumani", "Dang'ara tumani",
                    "Farg'ona tumani", "Furqat tumani", "Oltiariq tumani", "Qo'shtepa tumani", "Quva tumani",
                    "Rishton tumani", "So'x tumani", "Toshloq tumani", "Uchko'prik tumani", "O'zbekiston tumani",
                    "Yozyovon tumani")),
            new RegionSeed("1733", "Xorazm viloyati", List.of("Urganch shahri", "Xiva shahri", "Bog'ot tumani",
                    "Gurlan tumani", "Hazorasp tumani", "Xiva tumani", "Xonqa tumani", "Qo'shko'pir tumani",
                    "Shovot tumani", "Urganch tumani", "Yangiariq tumani", "Yangibozor tumani", "Tuproqqal'a tumani")),
            new RegionSeed("1726", "Toshkent shahri", List.of("Bektemir tumani", "Chilonzor tumani", "Mirobod tumani",
                    "Mirzo Ulug'bek tumani", "Olmazor tumani", "Sergeli tumani", "Shayxontohur tumani",
                    "Uchtepa tumani", "Yakkasaroy tumani", "Yangihayot tumani", "Yashnobod tumani",
                    "Yunusobod tumani")));

    private final RegionRepository regions;
    private final DistrictRepository districts;

    public TerritorySeeder(RegionRepository regions, DistrictRepository districts) {
        this.regions = regions;
        this.districts = districts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Region> byCode = new LinkedHashMap<>();
        regions.findAll().forEach(r -> byCode.put(r.getCode(), r));
        int addedRegions = 0;
        int addedDistricts = 0;
        for (RegionSeed seed : REGIONS) {
            Region region = byCode.get(seed.code());
            if (region == null) {
                region = new Region();
                region.setCode(seed.code());
                region.setNameUz(seed.name());
                region = regions.save(region);
                addedRegions++;
            }
            if (districts.existsByRegionId(region.getId())) continue;
            for (String name : seed.districts()) {
                District d = new District();
                d.setRegion(region);
                d.setNameUz(name);
                districts.save(d);
                addedDistricts++;
            }
        }
        if (addedRegions > 0 || addedDistricts > 0) {
            log.info("Hududlar ma'lumotnomasi: {} ta hudud, {} ta tuman qo'shildi", addedRegions, addedDistricts);
        }
    }
}
