package com.vietblog.infrastructure.config.seed;

import com.vietblog.infrastructure.config.seed.SeedModels.CommentSeed;
import com.vietblog.infrastructure.config.seed.SeedModels.PostSeed;

import java.util.List;

import static com.vietblog.infrastructure.config.seed.SeedModels.gallery;
import static com.vietblog.infrastructure.config.seed.SeedModels.img;

/**
 * Danh mục 11 bài viết chuyên sâu thuộc ba mảng Du lịch (4), Ẩm thực (4) và Đời sống (3).
 * Tích hợp tự nhiên các cụm ảnh lưới Collage (+1, +2, 2x2, 1+2, 2 cột) đúng chủ đề từng đoạn văn.
 */
public final class LifestyleTravelFoodPostsCatalog {

    private LifestyleTravelFoodPostsCatalog() {}

    public static List<PostSeed> posts() {
        return List.of(
                sanMayCauDatDaLat(),
                phoCoHoiAnMuaDenLong(),
                cungDuongBienDaNangSonTra(),
                caoNguyenDaHaGiang(),
                vanHoaPhoHaNoiBuoiSom(),
                banDoCaPheSpecialtySaigon(),
                amThucChoNoiCaiRang(),
                tinhHoaAmThucCoDoHue(),
                gocLamViecErgonomic(),
                canBangThucTapVaDoAnNamCuoi(),
                phuongPhapGhiChuSecondBrain()
        );
    }

    private static PostSeed sanMayCauDatDaLat() {
        String content = """
                <h2>Hành trình xuất phát lúc bốn giờ rưỡi sáng xuyên màn sương lạnh</h2>
                <p>Để kịp đón khoảnh khắc biển mây dày đặc nhất phủ kín thung lũng phía đông nam Đà Lạt, tụi mình rời trung tâm thành phố khi đèn đường dọc cung đường Trần Hưng Đạo — Hùng Vương vẫn còn vàng rực trong sương mù. Càng chạy về hướng Trại Mát và Xuân Trường, không khí càng se lạnh mang theo mùi nhựa thông ngai ngái rất đặc trưng của vùng cao nguyên lúc rạng đông.</p>
                <p>Khi những tia nắng đầu tiên vừa chạm qua rặng núi xa, cả biển mây trắng xóa bồng bềnh hiện ra ôm trọn lấy những luống chè xanh mướt trải dài tít tắp bên sườn đồi Cầu Đất.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80", "Ánh bình minh vàng rực chiếu qua thung lũng sương mù buổi sớm"),
                        img("https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1200&q=80", "Biển mây trắng bồng bềnh trôi lững lờ trên những triền đồi xanh"),
                        img("https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1200&q=80", "Nắng sớm xuyên qua tán rừng thông cao vút trên cung đường đèo"),
                        img("https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80", "Toàn cảnh núi đồi trùng điệp nhìn từ điểm săn mây trên cao"),
                        img("https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80", "Khoảnh khắc mặt trời ló dạng nhuộm hồng tầng mây mỏng"),
                        img("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80", "Không gian khoáng đạt và trong lành của vùng ngoại ô Đà Lạt")
                ) + """
                <h2>Thưởng thức ly cà phê Arabica nóng hổi bên hiên gỗ</h2>
                <p>Sau hơn một tiếng thong thả dạo bước giữa những luống chè còn đẫm sương đêm, không gì tuyệt vời hơn là ghé vào một quán gỗ nhỏ nằm nép mình bên sườn đồi và gọi một bình cà phê Arabica Cầu Đất pha thủ công. Vị chua thanh nhẹ nhàng hòa quyện cùng hậu vị ngọt dịu của hạt cà phê bản địa giúp đôi bàn tay lạnh cóng ấm dần lên trong nắng sớm.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=80", "Chiết xuất cà phê Arabica Cầu Đất nóng hổi bằng phễu giấy thủ công"),
                        img("https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=1200&q=80", "Góc bàn gỗ mộc mạc đón trọn ánh nắng ban mai dịu nhẹ"),
                        img("https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80", "Tách cà phê sữa ấm áp cho buổi sáng se lạnh vùng cao nguyên")
                ) + """
                <h2>Một vài lưu ý thực tế để chuyến đi trọn vẹn</h2>
                <ul>
                    <li><strong>Theo dõi độ ẩm và sức gió đêm hôm trước:</strong> Những đêm trời quang, ít gió và vừa có mưa nhẹ vào chiều hôm trước thường cho xác suất xuất hiện biển mây dày cao nhất vào sáng sớm hôm sau.</li>
                    <li><strong>Trang phục giữ ấm nhiều lớp:</strong> Nhiệt độ tại Cầu Đất lúc năm giờ sáng có thể xuống dưới mười bốn độ C, vì vậy bạn nên mang theo áo khoác cản gió, khăn choàng cổ và giày đế bám tốt để tránh trơn trượt trên đường đất ẩm.</li>
                    <li><strong>Di chuyển tốc độ vừa phải:</strong> Một số khúc cua qua đèo Trại Mát có sương mù che khuất tầm nhìn ngắn, hãy bật đèn sương mù vàng và giữ khoảng cách an toàn suốt hành trình.</li>
                </ul>
                """;

        return new PostSeed(
                "san-may-cau-dat-da-lat",
                "Săn mây đồi chè Cầu Đất và nhịp sống chậm giữa sương sớm Đà Lạt",
                "Trải nghiệm đón bình minh trên biển mây Cầu Đất, dạo bước giữa đồi chè xanh mướt và nhâm nhi tách Arabica nóng hổi trong tiết trời mười bốn độ.",
                content,
                "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1200&q=80",
                "du-lich",
                "lehongnhung@gmail.com",
                List.of("da-lat", "san-may", "cau-dat", "du-lich", "nhiep-anh"),
                11.8742, 108.5673, "Đồi chè Cầu Đất, Xã Xuân Trường, TP. Đà Lạt, Lâm Đồng",
                3680, 128, 1, 0, null, null,
                List.of(
                        CommentSeed.of("buihoangyen@gmail.com",
                                "Bộ ảnh sương sớm ở đồi chè đẹp mê hồn Nhung ơi! Nhìn ánh nắng xuyên qua rừng thông với góc cà phê gỗ mà chỉ muốn xách balo lên Đà Lạt ngay cuối tuần này.",
                                21, 7,
                                List.of(
                                        CommentSeed.of("lehongnhung@gmail.com",
                                                "Lên lịch liền đi Yến ơi, tháng này Đà Lạt đang vào mùa khô trời trong veo nên hầu như sáng nào lên Cầu Đất cũng bắt gặp biển mây dày hết á!",
                                                12, 5,
                                                List.of(
                                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                                "Cho mình hỏi đường từ trung tâm chạy ra Xuân Trường hiện tại mặt đường nhựa đã êm hết chưa hay còn đoạn nào đang sửa vậy hai bạn?",
                                                                6, 3)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("nguyenthutrang@gmail.com",
                                "Đợt trước mình đi muộn lúc gần 7 giờ sáng thì mây đã tan khá nhanh và nắng lên hơi gắt, đúng là phải chịu khó dậy từ 4 giờ rưỡi như bài viết mới săn được khoảnh khắc vàng.",
                                9, 2)
                )
        );
    }

    private static PostSeed phoCoHoiAnMuaDenLong() {
        String content = """
                <h2>Vẻ đẹp trầm mặc khi hoàng hôn buông xuống những mái ngói rêu phong</h2>
                <p>Khác hẳn với sự nhộn nhịp của những đô thị biển hiện đại, Hội An quyến rũ người lữ hành bằng sắc vàng nghệ ấm áp của những bức tường cổ kính phủ giàn hoa giấy và nhịp sống khoan thai trôi dọc đôi bờ sông Hoài. Thời điểm đẹp nhất để cảm nhận trọn vẹn thần thái phố Hội là khoảng năm giờ chiều, khi nắng chiều dịu dần trên mái ngói âm dương và những chiếc đèn lồng lụa đầu tiên bắt đầu thắp sáng trước hiên nhà.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1200&q=80", "Phố cổ Hội An rực rỡ sắc màu đèn lồng phản chiếu bên bờ sông"),
                        img("https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80", "Những chiếc thuyền gỗ nhỏ lững lờ trôi trong buổi chiều thanh bình"),
                        img("https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1200&q=80", "Nét kiến trúc truyền thống đặc trưng gắn liền với di sản văn hóa miền Trung"),
                        img("https://images.unsplash.com/photo-1540611025311-01df3cef54b5?auto=format&fit=crop&w=1200&q=80", "Góc phố nhỏ yên tĩnh rợp bóng cây xanh vào buổi chiều muộn"),
                        img("https://images.unsplash.com/photo-1508009603885-50cf7c579365?auto=format&fit=crop&w=1200&q=80", "Ánh đèn ấm áp tạo nên khung cảnh thơ mộng dọc tuyến phố đi bộ")
                ) + """
                <h2>Đêm rằm phố cổ và hương vị trà thảo mộc thanh mát</h2>
                <p>Vào mỗi tối mười bốn âm lịch hằng tháng, toàn bộ khu phố cổ đồng loạt tắt đèn điện hiện đại để nhường chỗ cho ánh nến hoa đăng lung linh trên mặt nước cùng tiếng đàn bầu, bài chòi vang vọng từ khu vực bùng binh An Hội. Giữa hành trình tản bộ, dừng chân thưởng thức một ly nước Mót thơm dịu mùi sả chanh lá sen và tô Cao lầu sợi dai giòn đậm vị là trải nghiệm khiến ai từng ghé qua cũng lưu luyến mãi.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=1200&q=80", "Tách trà thảo mộc thơm hương sả chanh thanh nhiệt giữa phố cổ"),
                        img("https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1200&q=80", "Thưởng thức các món ăn truyền thống đậm đà bản sắc xứ Quảng")
                ) + """
                <blockquote>
                    <p>Nếu muốn chụp được những khung hình phố cổ vắng người và trong trẻo nhất, hãy thức dậy dạo phố vào lúc sáu giờ sáng khi những gánh hàng rong vừa nhóm bếp bên góc đường Trần Phú.</p>
                </blockquote>
                """;

        return new PostSeed(
                "pho-co-hoi-an-mua-den-long",
                "Dạo bước Phố cổ Hội An đêm rằm: Ánh đèn lồng bên dòng sông Hoài trầm mặc",
                "Cảm nhận vẻ đẹp cổ kính của thương cảng Hội An từ buổi hoàng hôn vàng rực trên mái ngói âm dương đến đêm hội hoa đăng lung linh bên sông Hoài.",
                content,
                "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1200&q=80",
                "du-lich",
                "buihoangyen@gmail.com",
                List.of("hoi-an", "pho-co", "du-lich", "mien-trung", "di-san"),
                15.8771, 108.3260, "Chùa Cầu, Phố cổ Hội An, Tỉnh Quảng Nam",
                2910, 104, 1, 4, null, null,
                List.of(
                        CommentSeed.of("lehongnhung@gmail.com",
                                "Mẹo đi dạo phố lúc 6 giờ sáng của Yến chuẩn cực kỳ! Đợt mình ở lại đêm trong phố cổ, sáng sớm bước ra đường vắng lặng nghe tiếng chim hót bình yên không tả nổi.",
                                15, 60,
                                List.of(
                                        CommentSeed.of("buihoangyen@gmail.com",
                                                "Đúng rồi Nhung ha, tầm từ 8 giờ tối trở đi khách đoàn đổ về khu cầu An Hội khá đông nên đi sớm vừa thoáng vừa chụp ảnh màu nắng dịu đẹp hơn nhiều.",
                                                9, 54)
                                )
                        ),
                        CommentSeed.of("dangthimylinh@gmail.com",
                                "Tông màu vàng ấm của tường cổ Hội An kết hợp đèn lồng lên hình cuốn hút thật sự, cảm ơn bài viết giàu cảm xúc của tác giả nhé!",
                                8, 40)
                )
        );
    }

    private static PostSeed cungDuongBienDaNangSonTra() {
        String content = """
                <h2>Hành trình ôm trọn đường cong bờ biển đẹp nhất miền Trung</h2>
                <p>Xuất phát từ bãi biển Mỹ Khê với dải cát trắng mịn màng trải dài, cung đường Hoàng Sa dẫn lối người đi thẳng lên sườn núi xanh thẫm của bán đảo Sơn Trà — nơi được mệnh danh là lá phổi xanh nguyên sinh ôm ấp thành phố Đà Nẵng. Một bên là vách đá rợp bóng cây rừng nhiệt đới, một bên là đại dương xanh ngọc bích vỗ sóng rì rào dưới chân ghềnh tạo nên khung cảnh phóng khoáng hiếm nơi nào sánh kịp.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80", "Làn nước biển trong xanh như ngọc bích dọc bờ cát mịn Mỹ Khê"),
                        img("https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=1200&q=80", "Nắng vàng rực rỡ trải dài trên bãi biển buổi sớm mai"),
                        img("https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=1200&q=80", "Những con sóng bạc đầu vỗ nhẹ vào ghềnh đá ven bán đảo Sơn Trà"),
                        img("https://images.unsplash.com/photo-1473116763249-2faaef81ccda?auto=format&fit=crop&w=1200&q=80", "Không gian biển trời bình yên nhìn từ mỏm đá cao trên đèo"),
                        img("https://images.unsplash.com/photo-1468413253725-0d5181091126?auto=format&fit=crop&w=1200&q=80", "Hoàng hôn buông dần phía chân trời khép lại ngày khám phá đáng nhớ")
                ) + """
                <h2>Điểm dừng chân Chùa Linh Ứng và Đỉnh Bàn Cờ</h2>
                <p>Từ khoảng sân rộng thoáng đãng của Chùa Linh Ứng Bãi Bụt, phóng tầm mắt ra xa là toàn bộ đường chân trời thành phố Đà Nẵng với những cây cầu bắc qua sông Hàn hiện lên rõ nét giữa làn gió biển mát rượi. Nếu tiếp tục chinh phục cung đèo dốc dẫn lên Đỉnh Bàn Cờ bằng xe số, bạn sẽ được chiêm ngưỡng bức tranh giao hòa kỳ vĩ giữa núi rừng và biển cả từ độ cao gần bảy trăm mét.</p>
                <p>Lưu ý quan trọng theo quy định quản lý an toàn giao thông tại bán đảo Sơn Trà là du khách chỉ được phép sử dụng xe số hoặc xe côn tay khi lên các tuyến dốc cao phía sâu trong bán đảo, tuyệt đối không di chuyển bằng xe tay ga để đảm bảo an toàn hệ thống phanh khi đổ đèo.</p>
                """;

        return new PostSeed(
                "cung-duong-bien-da-nang-son-tra",
                "Cung đường biển Mỹ Khê — Bán đảo Sơn Trà: Khoảng thở xanh mát cho kỳ nghỉ cuối tuần",
                "Khám phá tuyến đường ven biển Hoàng Sa tuyệt đẹp nối liền bãi tắm Mỹ Khê tới Chùa Linh Ứng và Đỉnh Bàn Cờ trên bán đảo Sơn Trà.",
                content,
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
                "du-lich",
                "nguyenvanan@gmail.com",
                List.of("da-nang", "my-khe", "son-tra", "du-lich-bien", "cuoi-tuan"),
                16.1063, 108.2618, "Bán đảo Sơn Trà, Quận Sơn Trà, TP. Đà Nẵng",
                2630, 91, 2, 7, null, null,
                List.of(
                        CommentSeed.of("lehongnhung@gmail.com",
                                "Cảm ơn An đã nhắc chi tiết vụ quy định cấm xe tay ga lên đỉnh Bàn Cờ nhé! Nhiều bạn khách du lịch chưa biết chạy xe ga tới trạm gác phải quay đầu hoặc gửi xe lại khá bất tiện.",
                                14, 110,
                                List.of(
                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                "Đúng rồi Nhung, dốc đoạn từ trạm kiểm lâm lên cây đa ngàn năm khá gắt nên thuê một chiếc xe số khỏe vừa đúng quy định vừa yên tâm thắng số lúc xuống dốc.",
                                                10, 102,
                                                List.of(
                                                        CommentSeed.of("buihoangyen@gmail.com",
                                                                "Tháng 5 đến tháng 7 đi Sơn Trà may mắn còn bắt gặp từng đàn voọc chà vá chân nâu quý hiếm kiếm ăn trên tán cây ven đường nữa đó hai bạn!",
                                                                11, 96)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static PostSeed caoNguyenDaHaGiang() {
        String content = """
                <h2>Vẻ đẹp hùng vĩ nơi địa đầu Tổ quốc</h2>
                <p>Vượt qua con dốc Bắc Sum uốn lượn như dải lụa mềm vắt ngang sườn núi, cao nguyên đá Đồng Văn mở ra trước mắt lữ khách một thế giới hoàn toàn khác biệt với những dãy núi đá tai mèo xám bạc dựng đứng nối tiếp nhau đến tận chân trời. Giữa khung cảnh tưởng chừng khô cằn khắc nghiệt ấy, sức sống bền bỉ của đồng bào vùng cao vẫn nở hoa qua từng nương ngô xanh thẳm và những thảm hoa tam giác mạch phớt hồng dịu dàng trong gió thu.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1200&q=80", "Những đỉnh núi cao vút sừng sững dưới bầu trời trong trẻo vùng cực Bắc"),
                        img("https://images.unsplash.com/photo-1454496522488-7a8e488e8606?auto=format&fit=crop&w=1200&q=80", "Cung đèo quanh co ôm sát triền núi đá hiểm trở đầy mê hoặc"),
                        img("https://images.unsplash.com/photo-1486870591958-9b9d0d1dda99?auto=format&fit=crop&w=1200&q=80", "Ánh nắng ban mai trải vàng trên những dãy núi trùng điệp"),
                        img("https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80", "Hẻm vực sâu nhìn xuống dòng sông xanh ngọc bích uốn lượn dưới chân đèo"),
                        img("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80", "Mây trắng vờn quanh sườn đèo tạo nên bức tranh thiên nhiên kỳ vĩ")
                ) + """
                <h2>Khoảnh khắc lặng người trên đèo Mã Pí Lèng và dòng sông Nho Quế</h2>
                <p>Đứng từ trạm dừng chân trên đỉnh đèo Mã Pí Lèng nhìn xuống hẻm vực Tu Sản sâu hun hút bên dưới, dòng sông Nho Quế hiện lên như một sợi chỉ ngọc bích tĩnh lặng vắt ngang giữa hai vách đá dựng đứng cao hàng trăm mét. Buổi tối quay về phố cổ Đồng Văn, ngồi quây quần bên nồi thắng cố hay nồi lẩu gà đen nóng nghi ngút khói giữa cái lạnh mười hai độ là ký ức ấm áp chẳng thể nào quên.</p>
                """;

        return new PostSeed(
                "ha-giang-cao-nguyen-da",
                "Rong ruổi Cao nguyên đá Đồng Văn: Khúc tráng ca giữa mây trời cực Bắc",
                "Hành trình chinh phục đèo Mã Pí Lèng, ngắm dòng sông Nho Quế xanh màu ngọc bích và cảm nhận vẻ đẹp kiên cường trên miền đá nở hoa Hà Giang.",
                content,
                "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1200&q=80",
                "du-lich",
                "lehongnhung@gmail.com",
                List.of("ha-giang", "dong-van", "ma-pi-leng", "nho-que", "du-lich"),
                23.2425, 105.4119, "Đèo Mã Pí Lèng, Huyện Mèo Vạc, Tỉnh Hà Giang",
                2420, 89, 0, 16, null, null,
                List.of(
                        CommentSeed.of("phamminhduc@gmail.com",
                                "Nhìn góc ảnh chụp từ đỉnh đèo xuống hẻm vực mà nổi da gà vì quá hùng vĩ! Cuối tháng sau nhóm mình cũng đã chốt lịch bay ra Hà Nội rồi chạy cung Quản Bạ — Yên Minh — Đồng Văn.",
                                13, 240),
                        CommentSeed.of("trandinhtrong@gmail.com",
                                "Nếu tự cầm lái xe máy cung này mọi người nhớ kiểm tra kỹ lốp và má phanh trước khi xuất phát ở thành phố Hà Giang nhé, đường đèo cua tay áo liên tục đòi hỏi tập trung cao độ.",
                                9, 225)
                )
        );
    }

    private static PostSeed vanHoaPhoHaNoiBuoiSom() {
        String content = """
                <h2>Linh hồn của bát phở nằm trọn ở nồi nước dùng trong vắt</h2>
                <p>Người Hà Nội thưởng thức phở buổi sáng không vội vã mà như một nghi thức quen thuộc mở đầu ngày mới giữa tiết trời se lạnh. Bí quyết làm nên danh tiếng của những quán phở gia truyền lâu đời dọc phố Bát Đàn, Lý Quốc Sư hay Ấu Triệu nằm ở nồi nước dùng ninh suốt mười hai tiếng từ xương ống bò tươi cùng gừng nướng cháy cạnh, hành khô nướng thơm và một lượng thảo quả, hoa hồi tiết chế vừa đủ để nước trong veo mà hậu vị vẫn ngọt sâu thanh tao.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=1200&q=80", "Bát phở bò tái chín nóng hổi nghi ngút khói với hành hoa thái nhỏ"),
                        img("https://images.unsplash.com/photo-1503764654157-72d979d9af2f?auto=format&fit=crop&w=1200&q=80", "Sợi bánh phở mềm mượt hòa quyện trong làn nước dùng trong vắt đậm vị"),
                        img("https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80", "Thịt bò tươi thái mỏng trần vừa chín tới giữ trọn độ ngọt mềm"),
                        img("https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80", "Các nguyên liệu gia vị tự nhiên tạo nên chiều sâu hương thơm tinh tế"),
                        img("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80", "Bàn ăn sáng truyền thống giản dị mà đầy cuốn hút nơi góc phố cổ")
                ) + """
                <h2>Nét duyên của đĩa quẩy giòn và tách cà phê trứng sau bữa sáng</h2>
                <p>Một bát phở chuẩn vị phố cổ thường không kèm đĩa rau sống đầy đặn như miền Nam, mà chỉ điểm xuyết hành lá chẻ dọc, chút giấm tỏi ngâm chua dịu, tương ớt chưng cay nồng và vài chiếc quẩy nóng giòn chấm ngập trong nước phở béo ngậy. Ăn xong bát phở ấm bụng, tạt ngang quán nhỏ gần bờ hồ gọi thêm tách cà phê trứng đánh bông mịn màng thơm mùi bơ sữa là trọn vẹn phong vị buổi sáng đất Kinh kỳ.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80", "Tách cà phê trứng béo mịn phủ lớp bọt kem vàng ươm đặc trưng Hà Nội"),
                        img("https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=1200&q=80", "Góc quán nhỏ cổ kính nơi người dân thảnh thơi đọc báo sáng")
                );

        return new PostSeed(
                "van-hoa-pho-ha-noi-buoi-som",
                "Hương vị Phở Hà Nội buổi sớm: Tinh hoa từ nồi nước dùng ninh xương trong vắt",
                "Khám phá nét tinh tế trong cách ninh nước dùng thanh trong, thái thịt bò tái gầu chuẩn mực và thói quen nhâm nhi cà phê trứng sau bữa sáng phố cổ.",
                content,
                "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=1200&q=80",
                "am-thuc",
                "lehongnhung@gmail.com",
                List.of("pho-ha-noi", "am-thuc", "pho-co", "ca-phe-trung", "van-hoa"),
                21.0338, 105.8466, "Phố Bát Đàn, Quận Hoàn Kiếm, Hà Nội",
                3050, 108, 2, 2, null, null,
                List.of(
                        CommentSeed.of("doanhkhoa@gmail.com",
                                "Đọc đoạn tả giấm tỏi ớt chưng ăn kèm quẩy giòn chấm nước phở tái gầu mà đói cồn cào luôn! Phở Bắc ăn vào những ngày chớm thu se lạnh đúng là cực phẩm.",
                                17, 32,
                                List.of(
                                        CommentSeed.of("lehongnhung@gmail.com",
                                                "Chuẩn luôn Khoa ơi, cái vị chua thanh của giấm tỏi làm dậy mùi thơm của thịt bò tươi rõ rệt hơn hẳn so với vắt chanh thông thường.",
                                                10, 28,
                                                List.of(
                                                        CommentSeed.of("vuquoctuan@gmail.com",
                                                                "Lần đầu ra Hà Nội em chưa quen vụ xếp hàng tự bưng tô ở Bát Đàn, nhưng lúc nếm thử muỗng nước dùng đầu tiên thì công nhận bõ công đứng đợi thật sự!",
                                                                8, 22)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("ngothanhlong@gmail.com",
                                "Một số quán vỉa hè phố cổ vào giờ cao điểm buổi sáng bàn ghế quá chật, khâu thu dọn giấy ăn dưới sàn còn bẩn và chưa sạch như kỳ vọng của thực khách.",
                                4, 15)
                )
        );
    }

    private static PostSeed banDoCaPheSpecialtySaigon() {
        String content = """
                <h2>Tiêu chí chọn quán ngồi làm việc lý tưởng cho dân công nghệ</h2>
                <p>Với những lập trình viên hay nhà thiết kế thường xuyên muốn đổi gió làm việc ngoài văn phòng vào dịp cuối tuần, một quán cà phê tốt không chỉ dừng ở đồ uống ngon mà còn cần hội tụ đủ bốn yếu tố thực tế: mặt bàn gỗ đủ rộng và đúng tầm tay gõ phím, ổ cắm điện bố trí thuận tiện ở từng dãy ghế, đường truyền mạng ổn định và âm lượng nhạc nền vừa phải không gây xao nhãng luồng suy nghĩ.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=1200&q=80", "Không gian quán cà phê ngập tràn ánh sáng tự nhiên với trần cao thoáng đãng"),
                        img("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=1200&q=80", "Quầy pha chế Specialty gọn gàng với hương thơm hạt rang mộc lan tỏa"),
                        img("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=80", "Ly Pour-over chiết xuất thủ công giữ trọn hương hoa quả tươi sáng"),
                        img("https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=1200&q=80", "Bàn gỗ dài rộng rãi lý tưởng để mở laptop làm việc tập trung nhiều giờ"),
                        img("https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=1200&q=80", "Tách Latte nóng tạo hình nghệ thuật tinh tế đi kèm bánh nướng bơ Pháp"),
                        img("https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80", "Góc ngồi yên tĩnh cạnh khung cửa kính nhìn ra hàng cây xanh Quận 3")
                ) + """
                <h2>Ba khu vực tập trung nhiều quán yên tĩnh đáng ghé nhất tại TP.HCM</h2>
                <ul>
                    <li><strong>Khu vực Hồ Con Rùa — Phạm Ngọc Thạch (Quận 3):</strong> Quy tụ nhiều quán nằm ẩn mình trong các biệt thự Pháp cổ hoặc tầng lầu chung cư cũ, không gian tách biệt hẳn tiếng còi xe bên ngoài và phục vụ dòng Cold Brew ủ lạnh hương cam quế rất dễ chịu.</li>
                    <li><strong>Khu vực Thảo Điền (Thành phố Thủ Đức):</strong> Nổi bật với thiết kế sân vườn thoáng rộng nhiều cây xanh, ánh sáng tự nhiên dịu mắt suốt buổi sáng và có khu bàn dài riêng dành cho nhóm trao đổi dự án nhỏ.</li>
                    <li><strong>Khu vực Lê Văn Sỹ — Huỳnh Văn Bánh (Phú Nhuận — Tân Bình):</strong> Thuận tiện di chuyển cho các bạn sinh viên khu vực Tân Bình, mức giá đồ uống hợp lý cùng thái độ nhân viên thân thiện, luôn châm nước lọc ấm đều đặn.</li>
                </ul>
                """;

        return new PostSeed(
                "ban-do-ca-phe-specialty-saigon",
                "Bản đồ những quán Cà phê Specialty yên tĩnh, bàn rộng và ánh sáng đẹp để làm việc tại TP.HCM",
                "Tổng hợp những không gian cà phê rang xay thủ công có bàn cao chuẩn tư thế, ổ điện đầy đủ và bầu không khí tĩnh lặng cho ngày chạy dự án tập trung.",
                content,
                "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=1200&q=80",
                "am-thuc",
                "buihoangyen@gmail.com",
                List.of("ca-phe", "saigon", "specialty-coffee", "work-cafe", "am-thuc"),
                10.7828, 106.6959, "Đường Phạm Ngọc Thạch, Quận 3, TP.HCM",
                2790, 97, 1, 5, null, null,
                List.of(
                        CommentSeed.of("dangthimylinh@gmail.com",
                                "Lưu bài này gấp luôn Yến ơi! Cuối tuần mình hay mang laptop ra khu Hồ Con Rùa ngồi code giao diện, công nhận mấy quán trong hẻm biệt thự cổ ở Quận 3 yên tĩnh và mát mắt thật sự.",
                                14, 80,
                                List.of(
                                        CommentSeed.of("buihoangyen@gmail.com",
                                                "Hôm nào chủ nhật rảnh rủ thêm Trang ra ngồi chung bàn dài vừa làm việc vừa nhâm nhi bánh sừng bò nóng mới ra lò nha Linh!",
                                                9, 74,
                                                List.of(
                                                        CommentSeed.of("nguyenthutrang@gmail.com",
                                                                "Chốt lịch liền hai nàng ơi, mình xin chân gọi bình trà Ô long ủ lạnh với review thiết kế cùng mọi người nhé!",
                                                                7, 68)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("trandinhtrong@gmail.com",
                                "Mấy quán mặt tiền kê ghế đẩu thấp không có tựa lưng, ngồi gõ phím quá một tiếng là đau mỏi lưng rất tệ và kém tập trung hẳn.",
                                6, 52)
                )
        );
    }

    private static PostSeed amThucChoNoiCaiRang() {
        String content = """
                <h2>Nhịp sống thương hồ rộn ràng từ khi mặt trời vừa ló rạng</h2>
                <p>Từ bến Ninh Kiều xuống thuyền máy xuôi theo dòng sông Cần Thơ khoảng ba mươi phút lúc năm giờ rưỡi sáng, chợ nổi Cái Răng hiện ra sống động với hàng trăm ghe bầu chở đầy khóm, dưa hấu, bí đỏ và sầu riêng từ khắp miệt vườn miền Tây tụ hội. Nét văn hóa độc đáo nhất trên sông là những cây bẹo tre cao vút treo lủng lẳng đúng thứ nông sản mà ghe đang bán, giúp người mua nhận biết từ đằng xa giữa tiếng máy nổ rộn rã.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80", "Bữa sáng đậm chất phương Nam với hương vị tươi ngon từ nguyên liệu địa phương"),
                        img("https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1200&q=80", "Các món nướng và ăn kèm dân dã hấp dẫn thực khách phương xa"),
                        img("https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=1200&q=80", "Sắc màu tươi mát của rau củ và trái cây chín mọng miệt vườn sông nước"),
                        img("https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=1200&q=80", "Trải nghiệm thưởng thức ẩm thực nóng hổi ngay trong buổi sáng trong lành")
                ) + """
                <h2>Cảm giác chòng chành thú vị bên tô hủ tiếu ghe</h2>
                <p>Ghé sát mạn thuyền vào một chiếc ghe bán đồ ăn sáng, cô chủ quán thoăn thoắt trụng sợi hủ tiếu dai mềm, xếp thịt khìa thơm phức, rắc thêm hành phi vàng rụm rồi trao tận tay thực khách chỉ trong chưa đầy một phút. Vừa giữ thăng bằng tô hủ tiếu nóng hổi theo nhịp sóng vỗ nhẹ dưới mạn thuyền, vừa nhấp ngụm sữa đậu nành lá dứa hay ly cà phê sữa đá pha vợt đậm đà mới thấy hết sự hào sảng và chân tình của người dân Tây Đô.</p>
                """;

        return new PostSeed(
                "am-thuc-cho-noi-cai-rang",
                "Bữa sáng bồng bềnh trên Chợ nổi Cái Răng: Tô hủ tiếu nóng hổi và ly cà phê kho giữa sông Hậu",
                "Trải nghiệm nét văn hóa ẩm thực sông nước miền Tây Nam Bộ trên ghe nhỏ lúc bình minh cùng tô hủ tiếu đậm đà và trái cây miệt vườn tươi rói.",
                content,
                "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=1200&q=80",
                "am-thuc",
                "vuquoctuan@gmail.com",
                List.of("can-tho", "cho-noi-cai-rang", "mien-tay", "am-thuc", "hu-tieu"),
                10.0044, 105.7445, "Chợ nổi Cái Răng, Quận Cái Răng, TP. Cần Thơ",
                1980, 74, 1, 12, null, null,
                List.of(
                        CommentSeed.of("nguyenvanan@gmail.com",
                                "Tô hủ tiếu trên ghe nhìn giản dị vậy mà nước lèo nấu từ xương heo với củ cải ngọt thanh khó quên thật Tuấn ha! Đợt về quê bạn ở Cần Thơ mình ấn tượng mãi đĩa khóm gọt sẵn ngọt lịm.",
                                11, 165,
                                List.of(
                                        CommentSeed.of("vuquoctuan@gmail.com",
                                                "Đúng rồi An ơi, khóm Cầu Đúc ăn ngay trên ghe vừa giòn vừa không bị rát lưỡi chút nào, đợt tới rảnh cả nhóm làm chuyến về miền Tây cuối tuần nữa nhé!",
                                                8, 158)
                                )
                        )
                )
        );
    }

    private static PostSeed tinhHoaAmThucCoDoHue() {
        String content = """
                <h2>Sự cầu kỳ và hài hòa ngũ sắc trong từng món ăn xứ Huế</h2>
                <p>Ẩm thực cố đô Huế là sự giao thoa tinh tế giữa nghệ thuật chế biến tỉ mỉ chốn cung đình xưa và sự mộc mạc, đậm đà của nguyên liệu dân gian ven đầm phá Tam Giang. Người Huế nấu ăn không chỉ để no lòng mà còn chú trọng sự cân bằng âm dương hàn nhiệt, thể hiện rõ qua màu đỏ ấm của ớt chưng, sắc xanh tươi của rau thơm, vị cay ấm của sả gừng và hương thơm nồng đặc trưng của mắm ruốc chưng kỹ.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80", "Tô bún bò Huế sợi nhỏ chuẩn vị với giò heo, chả cua và nước dùng thơm nồng hương sả"),
                        img("https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80", "Các món bánh truyền thống bày biện khéo léo cùng chén nước mắm ớt xanh ngọt dịu"),
                        img("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=1200&q=80", "Hương vị nước dùng thanh ngọt đậm đà níu chân thực khách trong chiều mưa xứ Huế"),
                        img("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80", "Sự kết hợp hài hòa giữa rau sống tươi non và nguyên liệu bản địa thanh lành"),
                        img("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80", "Mâm quà chiều ấm cúng gồm bánh bèo chén, bánh nậm lá dong và bánh bột lọc")
                ) + """
                <h2>Quà chiều bên bờ Nam sông Hương và hẻm chè cung đình</h2>
                <p>Khác với tô bún bò ở nhiều tỉnh thành thường dùng sợi bún to, bún bò gốc Huế sử dụng sợi bún nhỏ vừa phải ăn kèm viên chả cua vàng ươm béo bùi và rau chuối bào trắng nõn. Khi chiều buông, ghé vào một quán nhỏ đường Nguyễn Bính hay Chi Lăng gọi một mâm bánh bèo chén rắc tôm chấy bông tơi, bánh nậm gói lá dong thơm dịu rồi tráng miệng bằng ly chè hạt sen bọc nhãn lồng thanh mát là trải nghiệm ẩm thực tròn đầy với mức chi phí vô cùng dễ chịu.</p>
                """;

        return new PostSeed(
                "tinh-hoa-am-thuc-co-do-hue",
                "Hành trình thưởng thức ẩm thực Cố đô Huế: Từ bún bò đậm vị sả ruốc đến mâm bánh bèo nậm lọc",
                "Khám phá chiều sâu văn hóa ẩm thực đất Thần kinh qua tô bún bò sợi nhỏ chả cua, các loại bánh gói lá truyền thống và chè sen nhãn lồng thanh tao.",
                content,
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80",
                "am-thuc",
                "lehongnhung@gmail.com",
                List.of("hue", "am-thuc-hue", "bun-bo-hue", "banh-beo", "co-do"),
                16.4637, 107.5909, "Đường Lê Lợi, Bờ Nam sông Hương, TP. Huế",
                2140, 83, 1, 19, null, null,
                List.of(
                        CommentSeed.of("buihoangyen@gmail.com",
                                "Mê nhất là mâm bánh bèo chén ở Huế, tôm chấy làm tơi mịn thơm phức chan thêm muỗng nước mắm ấm hơi cay nhẹ ăn bao nhiêu chén cũng không ngán!",
                                12, 290),
                        CommentSeed.of("hoangdieuanh@gmail.com",
                                "Nhắc tới chè Huế là nhớ liền món chè bột lọc heo quay vừa mặn vừa ngọt lạ miệng mà cuốn cực kỳ.",
                                7, 275)
                )
        );
    }

    private static PostSeed gocLamViecErgonomic() {
        String content = """
                <h2>Đầu tư đúng chỗ cho tám tiếng ngồi trước màn hình mỗi ngày</h2>
                <p>Đau mỏi cổ vai gáy, khô mỏi điều tiết mắt và tê cổ tay là những vấn đề sức khỏe nghề nghiệp phổ biến nhất đối với sinh viên công nghệ và lập trình viên làm việc cường độ cao. Thực tế, việc xây dựng một góc làm việc chuẩn công thái học không đòi hỏi những thiết bị đắt đỏ xa xỉ, mà bắt đầu từ việc căn chỉnh đúng tỷ lệ hình học giữa tầm mắt, độ cao mặt bàn và tư thế nâng đỡ thắt lưng.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1200&q=80", "Bố trí bàn làm việc tối giản giúp tập trung tối đa khi lập trình"),
                        img("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1200&q=80", "Màn hình rời đặt ngang tầm mắt trên tay nâng linh hoạt giải phóng diện tích mặt bàn"),
                        img("https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=1200&q=80", "Kết hợp laptop cùng bàn phím rời và chuột công thái học giữ cổ tay thẳng tự nhiên"),
                        img("https://images.unsplash.com/photo-1587831990711-23ca6441447b?auto=format&fit=crop&w=1200&q=80", "Ánh sáng đèn treo màn hình dịu nhẹ chống chói phản xạ vào buổi tối"),
                        img("https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?auto=format&fit=crop&w=1200&q=80", "Điểm xuyết chậu cây xanh nhỏ mang lại cảm giác thư thái giữa giờ làm việc")
                ) + """
                <h2>Bốn nguyên tắc căn chỉnh tư thế đơn giản mà hiệu quả rõ rệt</h2>
                <ul>
                    <li><strong>Độ cao cạnh trên màn hình ngang tầm mắt:</strong> Khi nhìn thẳng tự nhiên, mắt bạn nên hướng vào một phần ba phía trên của màn hình với khoảng cách từ năm mươi đến bảy mươi centimet để đốt sống cổ không bị cúi gập về phía trước.</li>
                    <li><strong>Khuỷu tay tạo góc mở chín mươi độ:</strong> Bàn phím và chuột nên đặt ở độ cao sao cho hai vai thả lỏng hoàn toàn, cẳng tay song song với sàn nhà và cổ tay tựa nhẹ trên kê tay mềm.</li>
                    <li><strong>Chiếu sáng gián tiếp cân bằng độ tương phản:</strong> Tránh làm việc chỉ với ánh sáng màn hình trong phòng tối om vì sự chênh lệch độ sáng lớn khiến đồng tử phải điều tiết liên tục gây nhức mắt nhanh chóng.</li>
                    <li><strong>Quy tắc nghỉ ngắn sau mỗi bốn mươi lăm phút:</strong> Dành hai phút đứng dậy vươn vai, đi lấy một cốc nước ấm và phóng tầm mắt ra khoảng cây xanh xa ngoài cửa sổ giúp tuần hoàn máu và sự tỉnh táo phục hồi đáng kể.</li>
                </ul>
                """;

        return new PostSeed(
                "goc-lam-viec-ergonomic",
                "Thiết lập góc làm việc Công thái học bảo vệ cột sống và thị lực cho kỹ sư phần mềm",
                "Hướng dẫn căn chỉnh độ cao màn hình rời, tư thế đặt cổ tay, ánh sáng chống mỏi mắt và thói quen vận động ngắn giúp làm việc bền bỉ lâu dài.",
                content,
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1200&q=80",
                "doi-song",
                "nguyenvanan@gmail.com",
                List.of("ergonomic", "desk-setup", "doi-song", "suc-khoe", "developer"),
                10.7932, 106.6754, "Đường Lê Văn Sỹ, Quận Phú Nhuận, TP.HCM",
                2560, 95, 1, 8, null, null,
                List.of(
                        CommentSeed.of("phamminhduc@gmail.com",
                                "Từ ngày sắm chiếc tay nâng màn hình rời kết hợp đèn treo chống lóa như An chia sẻ, mình hết hẳn chứng mỏi gáy mỗi đợt chạy dự án căng thẳng cuối kỳ!",
                                13, 130,
                                List.of(
                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                "Khoản đầu tư đáng đồng tiền bát gạo nhất luôn anh Đức ạ, mặt bàn dưới chân màn hình lại trống hẳn ra để đặt sổ tay ghi chú cực kỳ thoáng.",
                                                8, 124)
                                )
                        ),
                        CommentSeed.of("doanhkhoa@gmail.com",
                                "Hồi năm hai mình hay nằm gập người trên giường ôm laptop code, thói quen đó cực kỳ xấu và hại cột sống thắt lưng, may mà chỉnh lại tư thế bàn ghế kịp thời.",
                                7, 115)
                )
        );
    }

    private static PostSeed canBangThucTapVaDoAnNamCuoi() {
        String content = """
                <h2>Áp lực kép của sinh viên năm cuối ngành Công nghệ Thông tin</h2>
                <p>Bước vào học kỳ đầu năm tư, phần lớn sinh viên Công nghệ Thông tin đều đồng thời đảm nhận hai nhiệm vụ nặng ký: vừa tham gia thực tập thực tế tại doanh nghiệp phần mềm vào giờ hành chính, vừa tự tay thiết kế, lập trình và viết quyển báo cáo cho học phần Đồ án Chuyên ngành. Nếu không có kế hoạch phân bổ quỹ thời gian rõ ràng theo từng tuần, việc rơi vào trạng thái quá tải và dồn toàn bộ khối lượng công việc vào hai tuần sát hạn nộp là điều rất dễ xảy ra.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1434030216411-0b793f4b4173?auto=format&fit=crop&w=1200&q=80", "Lập bảng kế hoạch phân chia mục tiêu công việc rõ ràng theo từng tuần"),
                        img("https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1200&q=80", "Dành khung giờ buổi tối yên tĩnh để hoàn thiện tài liệu đặc tả và biểu đồ thiết kế"),
                        img("https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80", "Trao đổi tiến độ định kỳ cùng bạn đồng hành và giảng viên hướng dẫn")
                ) + """
                <h2>Chiến lược chia nhỏ mốc hoàn thành và viết tài liệu song song với mã nguồn</h2>
                <p>Thay vì đợi viết xong toàn bộ chức năng mới bắt tay vào gõ báo cáo thuyết minh, kinh nghiệm hiệu quả nhất là hoàn thành dứt điểm từng phân hệ theo chiều dọc: ngay khi đóng xong phân hệ xác thực hoặc tìm kiếm thông minh trong tuần đó, hãy dành hai tiếng chiều Chủ Nhật để vẽ luôn sơ đồ tuần tự và chụp lại kết quả kiểm thử khi kiến thức còn nóng hổi.</p>
                <blockquote>
                    <p>Mỗi buổi tối trong tuần chỉ cần duy trì đều đặn chín mươi phút tập trung tuyệt đối không mạng xã hội, sau tám tuần liên tục bạn sẽ bất ngờ với độ chỉn chu của cả sản phẩm lẫn quyển báo cáo.</p>
                </blockquote>
                """;

        return new PostSeed(
                "can-bang-thuc-tap-va-do-an-nam-cuoi",
                "Cân bằng giữa thực tập doanh nghiệp và hoàn thành Đồ án năm cuối ngành Công nghệ Thông tin",
                "Bí quyết quản lý thời gian theo tuần, viết thuyết minh song song quá trình lập trình và giữ nhịp độ bền bỉ cho sinh viên năm cuối CNTT.",
                content,
                "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?auto=format&fit=crop&w=1200&q=80",
                "doi-song",
                "vuquoctuan@gmail.com",
                List.of("sinh-vien", "cntt", "thuc-tap", "quan-ly-thoi-gian", "hcmunre"),
                10.7965, 106.6668, "Khuôn viên HCMUNRE, 236B Lê Văn Sỹ, Quận Tân Bình, TP.HCM",
                2310, 87, 0, 15, null, null,
                List.of(
                        CommentSeed.of("hoangdieuanh@gmail.com",
                                "Bài chia sẻ rất thực tế và chững chạc Tuấn ơi! Khi phỏng vấn các bạn thực tập sinh, doanh nghiệp đánh giá rất cao những bạn biết chủ động báo cáo lịch học ở trường sớm để quản lý sắp xếp công việc hài hòa.",
                                15, 210,
                                List.of(
                                        CommentSeed.of("vuquoctuan@gmail.com",
                                                "Dạ em cảm ơn chị Diệu Anh nhiều ạ! Đúng là khi mình trao đổi minh bạch lịch gặp giảng viên hướng dẫn từ đầu tuần thì các anh mentor ở công ty đều tạo điều kiện hỗ trợ rất nhiệt tình.",
                                                9, 200)
                                )
                        )
                )
        );
    }

    private static PostSeed phuongPhapGhiChuSecondBrain() {
        String content = """
                <h2>Vì sao đọc nhiều tài liệu kỹ thuật nhưng vẫn nhanh quên khi đụng việc thực tế</h2>
                <p>Mỗi ngày người làm phần mềm tiếp xúc với hàng chục luồng thông tin từ tài liệu chính thức của thư viện, bài phân tích kiến trúc, cách sửa một lỗi cấu hình hiếm gặp cho đến các ghi chú họp nhóm. Việc chỉ lưu hàng trăm đường dẫn vào thanh dấu trang trình duyệt mà không tóm lược lại bằng ngôn ngữ của chính mình khiến tri thức nằm rời rạc và rất khó tra cứu lại sau vài tháng.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1517842645767-c639042777db?auto=format&fit=crop&w=1200&q=80", "Tóm tắt nguyên lý cốt lõi vào sổ tay bằng sơ đồ tư duy ngắn gọn"),
                        img("https://images.unsplash.com/photo-1488190211105-8b0e65b80b4e?auto=format&fit=crop&w=1200&q=80", "Tổ chức kho tri thức số khoa học theo dự án và chủ đề chuyên môn")
                ) + """
                <h2>Áp dụng mô hình bốn ngăn PARA và ghi chú nguyên tử</h2>
                <p>Phương pháp chia kho lưu trữ thành bốn nhóm gồm Dự án đang chạy, Mảng trách nhiệm dài hạn, Tài nguyên tham khảo và Kho lưu trữ đã hoàn tất giúp mọi ghi chú luôn có vị trí rõ ràng ngay từ giây đầu tiên. Mỗi khi giải quyết xong một vấn đề kỹ thuật hóc búa, hãy viết một ghi chú ngắn khoảng mười dòng nêu rõ triệu chứng lỗi, nguyên nhân gốc rễ và đoạn lệnh khắc phục kèm nhãn từ khóa liên quan để biến trải nghiệm cá nhân thành tài sản tri thức dùng lại trọn đời.</p>
                """;

        return new PostSeed(
                "phuong-phap-second-brain-ky-su",
                "Xây dựng Hệ thống Ghi chú Cá nhân Second Brain giúp lập trình viên ghi nhớ sâu kiến thức mới",
                "Cách ứng dụng cấu trúc PARA và phương pháp ghi chú nguyên tử để chuyển hóa tài liệu đọc hằng ngày thành kho kinh nghiệm tra cứu tức thì.",
                content,
                "https://images.unsplash.com/photo-1517842645767-c639042777db?auto=format&fit=crop&w=1200&q=80",
                "doi-song",
                "dangthimylinh@gmail.com",
                List.of("second-brain", "ghi-chu", "hoc-tap", "productivity", "doi-song"),
                10.7801, 106.6997, "Đường sách Nguyễn Văn Bình, Quận 1, TP.HCM",
                1820, 69, 1, 22, null, null,
                List.of(
                        CommentSeed.of("nguyenthutrang@gmail.com",
                                "Thói quen viết lại bằng lời của mình sau khi đọc xong một bài nghiên cứu UX giúp mình nhớ lâu gấp ba lần so với chỉ tô sáng đoạn văn!",
                                10, 320),
                        CommentSeed.of("nguyenvanan@gmail.com",
                                "Từ lúc gom các ghi chú gỡ lỗi Docker và PostgreSQL về một thư mục Markdown có gắn thẻ, mỗi lần dựng dự án mới mình tiết kiệm được hàng giờ mò lại trên mạng.",
                                8, 305)
                )
        );
    }
}

