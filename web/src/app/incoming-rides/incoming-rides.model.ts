export type Location = {
  lat: number;
  lng: number;
  address: string;
}

export type Ride = {
  id: number;
  startLocation: Location;
  endLocation: Location;
  stops?: Location[];
  startTime: string;
}
