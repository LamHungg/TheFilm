import { FnbCombo } from '../models/combo.model';

/**
 * F&B add-on catalog sent to POST /api/v1/public/bookings/orders (fnbItems).
 * Backend stores supplied F&B lines into sales_order_line alongside real tickets.
 */
export const FNB_CATALOG: FnbCombo[] = [
  {
    id: 'fnb-01',
    name: 'Combo Solo TheFilm',
    description: '1 Bắp rang bơ truyền thống (Ngọt/Mặn) 64oz + 1 Nước ngọt có ga 32oz',
    price: 89000,
    imageUrl: 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400&auto=format&fit=crop&q=80',
    category: 'COMBO',
    quantity: 0,
  },
  {
    id: 'fnb-02',
    name: 'Combo Couple Cinema',
    description: '1 Bắp rang bơ lớn (Caramel/Phô mai) 85oz + 2 Nước ngọt có ga 32oz mát lạnh',
    price: 129000,
    imageUrl: 'https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=400&auto=format&fit=crop&q=80',
    category: 'COMBO',
    quantity: 0,
  },
  {
    id: 'fnb-03',
    name: 'Combo Family Movie Night',
    description: '2 Bắp lớn 85oz các vị + 3 Nước ngọt 32oz + 1 Bánh snack khoai tây giòn tan',
    price: 199000,
    imageUrl: 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=400&auto=format&fit=crop&q=80',
    category: 'COMBO',
    quantity: 0,
  },
  {
    id: 'fnb-04',
    name: 'Bắp Rang Bơ Vị Caramel Phô Mai',
    description: 'Bắp rang cao cấp nhập khẩu phủ lớp sốt caramel thơm lừng và phô mai đậm đà',
    price: 65000,
    imageUrl: 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400&auto=format&fit=crop&q=80',
    category: 'POPCORN',
    quantity: 0,
  },
  {
    id: 'fnb-05',
    name: 'Coke Zero / Sprite mát lạnh 32oz',
    description: 'Nước giải khát có ga mát lạnh sảng khoái với dung tích lớn',
    price: 39000,
    imageUrl: 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80',
    category: 'DRINK',
    quantity: 0,
  },
];

