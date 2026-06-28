let map;
let regionLayers = [];

function clearLayers() {
  regionLayers.forEach(l => map.removeLayer(l));
  regionLayers = [];
}

document.getElementById('load').addEventListener('click', () => {
  const token = document.getElementById('token').value.trim();
  if (!token) { alert('Please enter token'); return; }
  fetch('/recalltotem/regions?token=' + encodeURIComponent(token))
    .then(r => { if (!r.ok) throw new Error('Failed to fetch regions: ' + r.status); return r.json(); })
    .then(data => {
      document.getElementById('info').textContent = data.length + ' region(s)';
      if (!map) {
        map = L.map('map').setView([0,0], 2);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18 }).addTo(map);
      }
      clearLayers();
      const list = document.getElementById('list');
      list.innerHTML = '';
      data.forEach((r, i) => {
        const lat = r.center_z / 1000.0;
        const lon = r.center_x / 1000.0;
        const circle = L.circle([lat, lon], {
          radius: Math.max(1, r.radius_blocks),
          color: '#d9534f',
          fillColor: '#d9534f',
          fillOpacity: 0.12
        }).addTo(map);
        regionLayers.push(circle);

        const div = document.createElement('div');
        div.className = 'region';
        div.innerHTML = '<strong>#' + i + '</strong> <span class="meta">center=(' + r.center_x + ',' + r.center_z + ') radius=' + r.radius_blocks + ' dim=' + r.dimension + '</span>'
          + '<div>' + (r.admin_note ? '<em>Note:</em> ' + r.admin_note : '') + '</div>'
          + '<div>' + (r.deny_message ? '<em>Deny:</em> ' + r.deny_message : '') + '</div>'
          + '<div style="margin-top:6px;"><button data-idx="' + i + '">Export JSON</button></div>';
        list.appendChild(div);

        div.querySelector('button').addEventListener('click', () => {
          const blob = new Blob([JSON.stringify(r, null, 2)], { type: 'application/json' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'region-' + i + '.json';
          document.body.appendChild(a);
          a.click();
          a.remove();
          URL.revokeObjectURL(url);
        });
      });
      if (data.length > 0) {
        const first = data[0];
        map.setView([first.center_z / 1000.0, first.center_x / 1000.0], 6);
      }
    })
    .catch(err => alert(err.message));
});
