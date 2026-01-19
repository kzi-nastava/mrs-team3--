import {Component} from '@angular/core';

@Component({
  selector: 'app-util',
  imports: [],
  templateUrl: './util.html',
  styleUrl: './util.css',
})

export class Util {
  static async reverseGeocode(lat: number, lng: number): Promise<string> {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;

    try {
      const res = await fetch(url, { headers: { Accept: 'application/json' } });
      const data = await res.json();
      return data.display_name ?? `${lat}, ${lng}`;
    } catch (e) {
      console.error('Reverse geocoding error:', e);
      return `${lat}, ${lng}`;
    }
  }
}
