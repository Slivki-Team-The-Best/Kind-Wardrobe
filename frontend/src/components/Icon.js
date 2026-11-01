import {
  Camera,
  CircleCheck,
  Clock,
  DoorOpen,
  EyeOff,
  Image,
  Layers,
  Leaf,
  LogOut,
  Map,
  MapPin,
  Package,
  Pencil,
  Phone,
  Plus,
  Repeat,
  Search,
  Store,
  Trash2,
  Upload,
  User,
  X,
} from 'lucide-react';

const ICONS = {
  camera: Camera,
  checkCircle: CircleCheck,
  clock: Clock,
  door: DoorOpen,
  eyeOff: EyeOff,
  image: Image,
  layers: Layers,
  leaf: Leaf,
  logOut: LogOut,
  map: Map,
  mapPin: MapPin,
  package: Package,
  pencil: Pencil,
  phone: Phone,
  plus: Plus,
  repeat: Repeat,
  search: Search,
  store: Store,
  trash: Trash2,
  upload: Upload,
  user: User,
  x: X,
};

export default function Icon({ name, size = 18, className, strokeWidth = 1.75 }) {
  const Component = ICONS[name] ?? Package;
  return <Component size={size} strokeWidth={strokeWidth} className={className} aria-hidden="true" />;
}
