# verse_quiz

A command-line game that shows you Bible verses and asks you to name the book,
chapter, and verse each one comes from. It uses the Berean Standard Bible (BSB).

## Files

- `verse_quiz.sh`: the game.
- `BSB.tsv`: every verse of the BSB, one per line, in tab-separated columns:
  `Book`, `Chapter`, `Verse`, `Text`.

## Requirements

- Bash 4 or newer. Linux and WSL have it. macOS ships Bash 3.2, so install a
  newer version, for example with `brew install bash`.
- The standard tools `awk`, `sed`, `fold`, and `shuf`. `shuf` comes with Linux
  and WSL; on macOS, install it with `brew install coreutils`.

## Starting a game

From the project folder:

```
./verse_quiz.sh
```

If you get "Permission denied", make the script executable first with
`chmod +x verse_quiz.sh`, or run it with `bash verse_quiz.sh`.

## How to play

1. The game shows a verse.
2. Type your guess on one line, such as `John 3:16`, and press Enter.
3. Repeat for each verse (10 by default).
4. After the last verse, the game reveals the correct answers, the points you
   earned for each verse, and your total score.

The game doesn't tell you whether a guess was right until the end. Press
**Ctrl-D** at any prompt to quit early.

```
Verse 1 of 10
    If we confess our sins, He is faithful and just to forgive us our sins and
    to cleanse us from all unrighteousness.

  Guess (Book Chapter:Verse): 1 John 1:8
```

### Typing a guess

Write the book, then the chapter and verse, separated by a colon or a space:

| You type        | Read as             |
| --------------- | ------------------- |
| `John 3:16`     | John, chapter 3, verse 16 |
| `John 3 16`     | John, chapter 3, verse 16 |
| `1 John 1.9`    | 1 John, chapter 1, verse 9 |
| `1john1:9`      | 1 John, chapter 1, verse 9 |
| `John 3`        | John, chapter 3, no verse |
| `John`          | John, no chapter or verse |

If you leave part off, you still get points for the parts you wrote. For
example, `John 3` can earn the book and chapter points. A verse can't be given
without a chapter: in `John 16`, the 16 is read as the chapter.

### Typing book names

Capitalization and spacing don't matter. Numbered books can be written several
ways: `1 John`, `1John`, `First John`, `1st John`, and `I John` all count.
These alternate names also work:

| You can type             | For                |
| ------------------------ | ------------------ |
| `Revelation`             | Revelation of John |
| `Song of Songs`          | Song of Solomon    |
| `Psalm`                  | Psalms             |

Chapter and verse numbers must be whole numbers.

## Scoring

Each verse is worth up to 25 points.

| Part    | Points | How it's earned |
| ------- | ------ | --------------- |
| Book    | 1      | The book is correct. |
| Chapter | 4      | Only if the book is correct. You lose 1 point for each number you're off, down to 0. |
| Verse   | 20     | Only if the book **and** chapter are exactly correct. You lose 5 points for each number you're off, down to 0. |

The chapter and verse points together are called **combo points**.

Examples, for an answer of John 3:16:

| Your guess | Book | Chapter | Verse | Total |
| ---------- | ---- | ------- | ----- | ----- |
| John 3:16  | 1    | 4       | 20    | 25    |
| John 3:18  | 1    | 4       | 10    | 15    |
| John 5:16  | 1    | 2       | 0     | 3     |
| Luke 3:16  | 0    | 0       | 0     | 0     |

On the results screen, ✓ means full points, `~` means partial points, and ✗
means no points.

## Options

| Option                    | What it does |
| ------------------------- | ------------ |
| `-o`, `--old`             | Only verses from the Old Testament (Genesis–Malachi). |
| `-n`, `--new`             | Only verses from the New Testament (Matthew–Revelation). |
| `-b`, `--book NAME`       | Only verses from one book. See below. |
| `-c`, `--count N`         | Play `N` verses instead of 10. |
| `-h`, `--help`            | Show the options. |

You can use only one of `--old`, `--new`, and `--book` at a time. `--count`
works with any of them.

```
./verse_quiz.sh --old                  # 10 Old Testament verses
./verse_quiz.sh -n -c 25               # 25 New Testament verses
./verse_quiz.sh --book "Romans"        # 10 verses from Romans
./verse_quiz.sh -b "1 john" -c 5       # 5 verses from 1 John
```

### Playing a single book

With `--book`, the game doesn't ask for the book: type just the chapter and
verse, such as `3:23` or `3 23`. If you type a book name anyway, it's ignored.
The book's 1 point moves to
the chapter instead, so the chapter is worth **5** points, minus 1 for each
number you're off. The verse is still worth 20 points and still requires the
exact chapter. Each verse is still worth up to 25 points.

Put book names with spaces in quotes, such as `--book "Song of Solomon"`. Book
names are matched the same way as your guesses, so `--book "first john"`
works. If the name isn't recognized, the game lists every book.

If you ask for more verses than the book has, the game uses all of them. For
example, `-b "3 John" -c 50` plays all 14 verses of 3 John.

### Using a different verse file

To use another file in the same format, pass its path as the last argument:

```
./verse_quiz.sh --new path/to/other.tsv
```

The file must be tab-separated with a header row and the columns `Book`,
`Chapter`, `Verse`, `Text`. Books must appear in canonical order, because
`--old` and `--new` treat the first 39 books as the Old Testament.

## Notes

The BSB leaves out 16 verses that don't appear in the oldest manuscripts, such
as Matthew 17:21 and John 5:4. They appear in `BSB.tsv` with no text, and the
game never asks about them.

## Disclaimer

This project was created jointly by Ry Schnitzl and Claude Opus 5.5
(`claude-opus-5-5`), an AI model made by Anthropic. The Berean Standard Bible
is in the public domain.
