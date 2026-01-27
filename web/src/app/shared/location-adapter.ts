export async function toBookingLocation(loc: {
  address: string;
  lat?: any;
  lng?: any;
  latitude?: any;
  longitude?: any;
}) {
  const lat = loc.lat ?? loc.latitude;
  const lng = loc.lng ?? loc.longitude;

  if (lat != null && lng != null && !isNaN(lat) && !isNaN(lng)) {
    return {
      lat: Number(lat),
      lng: Number(lng),
      name: loc.address
    };
  }

  // fallback: geocode by address
  const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
    loc.address + ', Serbia'
  )}&limit=1`;

  const res = await fetch(url);
  const data = await res.json();

  if (!data?.length) {
    throw new Error('Geocoding failed for ' + loc.address);
  }

  return {
    lat: Number(data[0].lat),
    lng: Number(data[0].lon),
    name: loc.address
  };
}
