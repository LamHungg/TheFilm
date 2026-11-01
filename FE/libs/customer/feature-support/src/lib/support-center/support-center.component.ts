import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  sender: 'USER' | 'BOT';
  text: string;
  time: string;
}

@Component({
  selector: 'thefilm-support-center',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support-center.component.html',
  styleUrl: './support-center.component.scss',
})
export class SupportCenterComponent implements AfterViewChecked {
  @ViewChild('chatContainer') chatContainer!: ElementRef;

  activeFaqIndex: number | null = 0;

  faqList = [
    {
      q: 'Làm thế nào để lấy vé sau khi đặt thành công trên website?',
      a: 'Sau khi thanh toán thành công, hệ thống sẽ cấp Mã đặt vé và Mã QR điện tử (lưu tại mục "Vé của tôi"). Quý khách chỉ cần mở mã QR tại cổng soát vé hoặc nhập mã tại Kiosk in vé tự động ở sảnh rạp.',
    },
    {
      q: 'Quy định đổi/trả vé đã mua như thế nào?',
      a: 'Vé đã thanh toán thành công được hỗ trợ đổi suất chiếu trước giờ phim bắt đầu tối thiểu 60 phút (tùy thuộc vào chính sách loại vé và rạp). Quý khách có thể liên hệ hotline 1900 6868 hoặc chat với hỗ trợ viên để được xử lý.',
    },
    {
      q: 'Quy định về độ tuổi phân loại phim (T13, T16, T18, P)?',
      a: 'P: Phim cho mọi lứa tuổi; T13: Người xem từ đủ 13 tuổi trở lên; T16: Người xem từ đủ 16 tuổi trở lên; T18: Người xem từ đủ 18 tuổi trở lên. Nhân viên soát vé có quyền yêu cầu xuất trình CCCD/giấy tờ tùy thân để kiểm tra độ tuổi theo quy định của Cục Điện ảnh.',
    },
    {
      q: 'Làm sao để xuất hóa đơn VAT điện tử?',
      a: 'Tại bước điền thông tin người nhận vé (Bước 3), tích chọn "Yêu cầu xuất hóa đơn điện tử VAT (Thuế suất GTGT 8%)" và điền đầy đủ Tên công ty, Mã số thuế, Địa chỉ. Hóa đơn điện tử hợp lệ sẽ được gửi về email trong vòng 24 giờ làm việc.',
    },
  ];

  messages: ChatMessage[] = [
    {
      sender: 'BOT',
      text: 'Xin chào! Tôi là Trợ lý ảo TheFilm AI. Tôi có thể giúp bạn kiểm tra suất chiếu, tra cứu mã vé, hướng dẫn thanh toán hoặc tư vấn chương trình ưu đãi hôm nay. Bạn cần hỗ trợ gì ạ?',
      time: 'Vừa xong',
    },
  ];

  userInput = '';

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleFaq(index: number): void {
    this.activeFaqIndex = this.activeFaqIndex === index ? null : index;
  }

  sendPreset(text: string): void {
    this.userInput = text;
    this.sendMessage();
  }

  sendMessage(): void {
    const text = this.userInput.trim();
    if (!text) return;

    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.messages.push({ sender: 'USER', text, time });
    this.userInput = '';

    // Generate intelligent AI Response
    setTimeout(() => {
      const botResponse = this.generateBotReply(text);
      this.messages.push({
        sender: 'BOT',
        text: botResponse,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      });
    }, 600);
  }

  private generateBotReply(query: string): string {
    const q = query.toLowerCase();

    if (q.includes('tra cứu') || q.includes('mã vé') || q.includes('vé đã mua')) {
      return 'Để tra cứu vé đã mua, bạn vui lòng bấm vào nút "VÉ CỦA TÔI" trên thanh điều hướng hoặc vào đường dẫn /my-tickets. Bạn có thể tra cứu theo Mã đặt vé (VD: TFM-842-990) hoặc xem trực tiếp mã QR.';
    }

    if (q.includes('đổi') || q.includes('hủy') || q.includes('hoàn tiền')) {
      return 'Theo quy chế nghiệp vụ rạp THEFILM, vé đã mua được đổi suất chiếu trước 60 phút trước giờ khởi chiếu. Bạn có thể gọi hotline 1900 6868 hoặc gửi email CSKH để được nhân viên tiếp nhận đối soát theo quy trình.';
    }

    if (q.includes('imax') || q.includes('laser')) {
      return 'Hiện tại cụm rạp THEFILM Nguyễn Trãi và THEFILM Landmark 81 đang có phòng chiếu IMAX Laser cao cấp với các phim bom tấn "Tầng Mây Cuối Cùng" và "Chiến Binh Ánh Trăng". Bạn hãy vào mục "Phim" để đặt vé nhé!';
    }

    if (q.includes('bắp') || q.includes('nước') || q.includes('combo')) {
      return 'THEFILM có các combo: Combo Solo (89.000₫), Combo Couple (129.000₫) và Family (199.000₫). Bạn có thể thêm trực tiếp bắp nước tại Bước 2 khi đặt vé.';
    }

    if (q.includes('voucher') || q.includes('giảm giá') || q.includes('khuyến mãi')) {
      return 'Bạn có thể sử dụng mã ưu đãi "THEFILM20" (giảm 20%) hoặc mã "VIPMEMBER" (giảm 40.000₫) khi thanh toán đơn vé tại rạp.';
    }

    return `Cảm ơn câu hỏi của bạn về "${query}". Hệ thống đã ghi nhận yêu cầu. Bạn có thể xem thêm ở mục FAQ bên trái hoặc gọi hotline 1900 6868 để gặp trực tiếp nhân viên tư vấn.`;
  }

  private scrollToBottom(): void {
    try {
      if (this.chatContainer) {
        this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
      }
    } catch (err) {}
  }
}
