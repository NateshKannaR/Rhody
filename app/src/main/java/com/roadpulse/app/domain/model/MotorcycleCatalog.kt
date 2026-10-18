package com.roadpulse.app.domain.model

data class MotorcycleModel(
    val brand: String,
    val model: String,
    val engineSpec: String,
    val category: String // "Honda", "Kawasaki", "Yamaha", "KTM", "Royal Enfield", "Bajaj", "TVS", "Hero", "Superbikes"
)

object MotorcycleCatalog {
    val brands = listOf("ALL", "HONDA", "KAWASAKI", "YAMAHA", "KTM", "ROYAL ENFIELD", "BAJAJ", "TVS", "HERO", "SUPERBIKES")

    val allBikes: List<MotorcycleModel> = listOf(
        // ================= HONDA =================
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CBR 1000RR-R Fireblade SP",
            engineSpec = "999cc Liquid Inline-4 • 217.5 PS • 113 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CBR 600RR",
            engineSpec = "599cc Liquid Inline-4 • 121 PS • 64 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CBR 300R",
            engineSpec = "286cc Liquid Single • 30.4 PS • 27.4 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CBR 250R",
            engineSpec = "249.6cc Liquid Single • 26.5 PS • 22.9 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CBR 150R",
            engineSpec = "149.16cc DOHC Liquid • 17.1 PS • 14.4 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CRF1100L Africa Twin",
            engineSpec = "1084cc Parallel Twin • 101 bhp • 105 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda Hornet 2.0",
            engineSpec = "184.4cc PGM-FI Air Cooled • 17.26 PS • 16.1 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda CB350 RS",
            engineSpec = "348.36cc PGM-FI Single • 21.07 PS • 30 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda H'ness CB350",
            engineSpec = "348.36cc PGM-FI Single • 21.07 PS • 30 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda SP 125",
            engineSpec = "123.94cc eSP PGM-FI • 10.87 PS • 10.9 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda Shine 125",
            engineSpec = "123.94cc eSP Air Cooled • 10.74 PS • 11 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda Activa 6G",
            engineSpec = "109.51cc PGM-FI eSP • 7.84 PS • 8.9 Nm",
            category = "HONDA"
        ),
        MotorcycleModel(
            brand = "Honda",
            model = "Honda Dio 125",
            engineSpec = "123.92cc PGM-FI eSP • 8.28 PS • 10.4 Nm",
            category = "HONDA"
        ),

        // ================= KAWASAKI =================
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja H2",
            engineSpec = "998cc Supercharged Inline-4 • 231 PS • 141.7 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja H2R",
            engineSpec = "998cc Supercharged Inline-4 (Track) • 310 PS • 165 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja ZX-10R",
            engineSpec = "998cc Liquid Inline-4 • 203 PS • 114.9 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja ZX-6R",
            engineSpec = "636cc Liquid Inline-4 • 124 PS • 69 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja ZX-4R",
            engineSpec = "399cc Liquid Inline-4 • 77 PS • 39 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja 400",
            engineSpec = "399cc Parallel Twin • 45 PS • 37 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja 500",
            engineSpec = "451cc Parallel Twin • 45.4 PS • 42.6 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja 650",
            engineSpec = "649cc Parallel Twin • 68 PS • 64 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja 300",
            engineSpec = "296cc Parallel Twin • 39 PS • 26.1 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Ninja 250",
            engineSpec = "249cc Parallel Twin • 39 PS • 23.5 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Z900",
            engineSpec = "948cc Liquid Inline-4 • 125 PS • 98.6 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Z1000",
            engineSpec = "1043cc Liquid Inline-4 • 142 PS • 111 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Z650",
            engineSpec = "649cc Parallel Twin • 68 PS • 64 Nm",
            category = "KAWASAKI"
        ),
        MotorcycleModel(
            brand = "Kawasaki",
            model = "Kawasaki Versys 650",
            engineSpec = "649cc Parallel Twin Adventure • 66 PS • 61 Nm",
            category = "KAWASAKI"
        ),

        // ================= YAMAHA =================
        MotorcycleModel(
            brand = "Yamaha",
            model = "Yamaha R15 V4",
            engineSpec = "155cc Liquid VVA • 18.4 PS • 14.2 Nm",
            category = "YAMAHA"
        ),
        MotorcycleModel(
            brand = "Yamaha",
            model = "Yamaha MT-15 V2",
            engineSpec = "155cc Liquid VVA • 18.4 PS • 14.1 Nm",
            category = "YAMAHA"
        ),
        MotorcycleModel(
            brand = "Yamaha",
            model = "Yamaha R3",
            engineSpec = "321cc Liquid Parallel Twin • 42 PS • 29.5 Nm",
            category = "YAMAHA"
        ),
        MotorcycleModel(
            brand = "Yamaha",
            model = "Yamaha YZF-R1",
            engineSpec = "998cc Crossplane Inline-4 • 200 PS • 112.4 Nm",
            category = "YAMAHA"
        ),

        // ================= KTM =================
        MotorcycleModel(
            brand = "KTM",
            model = "KTM Duke 390",
            engineSpec = "399cc Liquid DOHC Single • 46 PS • 39 Nm",
            category = "KTM"
        ),
        MotorcycleModel(
            brand = "KTM",
            model = "KTM RC 390",
            engineSpec = "373.2cc Liquid Single • 43.5 PS • 37 Nm",
            category = "KTM"
        ),
        MotorcycleModel(
            brand = "KTM",
            model = "KTM Duke 200",
            engineSpec = "199.5cc Liquid Single • 25 PS • 19.3 Nm",
            category = "KTM"
        ),

        // ================= ROYAL ENFIELD =================
        MotorcycleModel(
            brand = "Royal Enfield",
            model = "Royal Enfield Classic 350",
            engineSpec = "349cc J-Series Single • 20.2 bhp • 27 Nm",
            category = "ROYAL ENFIELD"
        ),
        MotorcycleModel(
            brand = "Royal Enfield",
            model = "Royal Enfield Bullet 350",
            engineSpec = "349cc J-Series Single • 20.2 bhp • 27 Nm",
            category = "ROYAL ENFIELD"
        ),
        MotorcycleModel(
            brand = "Royal Enfield",
            model = "Royal Enfield Continental GT 650",
            engineSpec = "648cc Parallel Twin • 47 PS • 52 Nm",
            category = "ROYAL ENFIELD"
        ),
        MotorcycleModel(
            brand = "Royal Enfield",
            model = "Royal Enfield Hunter 350",
            engineSpec = "349cc J-Series Single • 20.2 bhp • 27 Nm",
            category = "ROYAL ENFIELD"
        ),
        MotorcycleModel(
            brand = "Royal Enfield",
            model = "Royal Enfield Himalayan 450",
            engineSpec = "452cc Sherpa Liquid Single • 40.02 PS • 40 Nm",
            category = "ROYAL ENFIELD"
        ),

        // ================= BAJAJ =================
        MotorcycleModel(
            brand = "Bajaj",
            model = "Bajaj Pulsar 150",
            engineSpec = "149.5cc DTS-i Single • 14 PS • 13.25 Nm",
            category = "BAJAJ"
        ),
        MotorcycleModel(
            brand = "Bajaj",
            model = "Bajaj Pulsar NS 200",
            engineSpec = "199.5cc Triple Spark 4V Liquid • 24.5 PS • 18.74 Nm",
            category = "BAJAJ"
        ),
        MotorcycleModel(
            brand = "Bajaj",
            model = "Bajaj Pulsar 220F",
            engineSpec = "220cc DTS-i Oil Cooled • 20.4 PS • 18.55 Nm",
            category = "BAJAJ"
        ),
        MotorcycleModel(
            brand = "Bajaj",
            model = "Bajaj Dominar 400",
            engineSpec = "373.3cc DOHC Liquid • 40 PS • 35 Nm",
            category = "BAJAJ"
        ),

        // ================= TVS & HERO =================
        MotorcycleModel(
            brand = "TVS",
            model = "TVS Apache RTR 200 4V",
            engineSpec = "197.75cc 4V Oil Cooled • 20.8 PS • 17.25 Nm",
            category = "TVS"
        ),
        MotorcycleModel(
            brand = "TVS",
            model = "TVS Apache RR 310",
            engineSpec = "312.2cc Reverse Inclined Liquid • 34 PS • 27.3 Nm",
            category = "TVS"
        ),
        MotorcycleModel(
            brand = "Hero",
            model = "Hero Splendor Plus",
            engineSpec = "97.2cc Air Cooled • 8.02 PS • 8.05 Nm",
            category = "HERO"
        ),
        MotorcycleModel(
            brand = "Hero",
            model = "Hero Xpulse 200 4V",
            engineSpec = "199.6cc 4V Oil Cooled • 19.1 PS • 17.35 Nm",
            category = "HERO"
        ),

        // ================= SUPERBIKES =================
        MotorcycleModel(
            brand = "BMW",
            model = "BMW S1000 RR",
            engineSpec = "999cc ShiftCam Inline-4 • 210 PS • 113 Nm",
            category = "SUPERBIKES"
        ),
        MotorcycleModel(
            brand = "Ducati",
            model = "Ducati Panigale V4",
            engineSpec = "1103cc Desmosedici Stradale V4 • 215.5 PS • 123.6 Nm",
            category = "SUPERBIKES"
        ),
        MotorcycleModel(
            brand = "Suzuki",
            model = "Suzuki Hayabusa",
            engineSpec = "1340cc Liquid Inline-4 • 190 PS • 150 Nm",
            category = "SUPERBIKES"
        ),
        MotorcycleModel(
            brand = "Triumph",
            model = "Triumph Street Triple 765",
            engineSpec = "765cc Moto2 Derived Triple • 130 PS • 80 Nm",
            category = "SUPERBIKES"
        ),
        MotorcycleModel(
            brand = "Harley-Davidson",
            model = "Harley-Davidson Fat Boy 114",
            engineSpec = "1868cc Milwaukee-Eight 114 V-Twin • 94 HP • 155 Nm",
            category = "SUPERBIKES"
        )
    )
}
