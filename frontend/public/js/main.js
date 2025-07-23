console.log('Frontend ready');

function togglePassword(id) {
  const input = document.getElementById(id);
  if (input.type === 'password') input.type = 'text';
  else input.type = 'password';
}
