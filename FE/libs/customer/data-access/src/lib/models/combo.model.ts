export interface FnbCombo {
  id: string;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  category: 'COMBO' | 'POPCORN' | 'DRINK';
  quantity: number;
}
