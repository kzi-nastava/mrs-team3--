export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  surname: string;
  phoneNumber: string;
  address: string;
  base64Image?: string;
  extension?: string;
}
