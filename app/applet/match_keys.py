import subprocess

candidates = [
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '@', '#', '$', '%', '&', '-', '+', '(', ')', '/',
    '*', '"', "'", ':', ';', '!', '?', '~', '`', '|',
    '\\', '^', '=', '<', '>', '{', '}', '[', ']',
    '£', '€', '¥', '¢', '₹', '¿', '¡', '،', '؛', '؟',
    '_', '—', '–', '°', '•', '…', '‰', '§', '«', '»'
]

def best_match(key_path):
    best_char = None
    best_score = 999999
    for ch in candidates:
        cmd = ['convert', '-size', '90x90', 'xc:white', '-font', 'DejaVu-Sans',
               '-pointsize', '44', '-fill', 'black', '-gravity', 'center',
               '-annotate', '+0+0', ch, 'cand.png']
        subprocess.run(cmd, capture_output=True)
        cmd = ['compare', '-metric', 'RMSE', key_path, 'cand.png', 'null:']
        res = subprocess.run(cmd, capture_output=True, text=True)
        try:
            score = float(res.stderr.split()[0])
            if score < best_score:
                best_score = score
                best_char = ch
        except:
            pass
    return best_char, best_score

for r in range(1, 5):
    row_chars = []
    for k in range(10):
        ch, score = best_match(f'key_r{r}_k{k}.png')
        row_chars.append(f'{ch}({int(score)})')
    print(f'Row {r}:', ' '.join(row_chars))
