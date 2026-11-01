function initials(name) {
  return (name || '?')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}

export default function Avatar({ url, name, size = 32 }) {
  return (
    <span className="avatar-circle" style={{ width: size, height: size, fontSize: Math.round(size * 0.38) }}>
      {url ? <img src={url} alt="" /> : initials(name)}
    </span>
  );
}
