#!/usr/bin/env bash
# Verse quiz: shows random verses from BSB.tsv (10 by default, see --count)
# and asks for the book, chapter, and verse of each. Answers and the score are revealed at the end.
#
# Scoring per verse:
#   Book:    1 point if correct.
#   Combo points (only awarded when the book is correct):
#     Chapter: 4 points minus how far off the guess is, never below 0.
#     Verse:  20 points minus 5 for each number off, never below 0.
#             Only awarded when the chapter is also exactly right.
#
# When playing a single book (--book), the book isn't asked and its point is
# added to the chapter, which is then worth 5.
#
# Usage: ./verse_quiz.sh [--old | --new | --book NAME] [--count N] [path/to/BSB.tsv]

set -euo pipefail

DEFAULT_ROUNDS=10
ROUNDS=$DEFAULT_ROUNDS
BOOK_POINTS=1
CHAPTER_POINTS=4
CHAPTER_PENALTY=1
VERSE_POINTS=20
VERSE_PENALTY=5
OLD_TESTAMENT_BOOKS=39   # Genesis through Malachi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TSV="$SCRIPT_DIR/BSB.tsv"
SCOPE="all"
BOOK_ARG=""

usage() {
    cat <<EOF
Usage: ./verse_quiz.sh [options] [path/to/BSB.tsv]

Which verses (pick at most one):
  -o, --old          Only verses from the Old Testament
  -n, --new          Only verses from the New Testament
  -b, --book NAME    Only verses from one book, e.g. --book "1 John".
                     The book isn't asked; its point goes to the chapter.

Other options:
  -c, --count N      Number of verses in the game (default $DEFAULT_ROUNDS)
  -h, --help         Show this help
EOF
}

set_scope() {
    if [[ "$SCOPE" != "all" && "$SCOPE" != "$1" ]]; then
        echo "Choose only one of --old, --new, or --book." >&2
        exit 1
    fi
    SCOPE="$1"
}

while (( $# )); do
    case "$1" in
        -o|--old) set_scope old ;;
        -n|--new) set_scope new ;;
        -b|--book)
            if (( $# < 2 )); then
                echo "--book needs a book name, e.g. --book \"1 John\"." >&2
                exit 1
            fi
            set_scope book
            BOOK_ARG="$2"
            shift
            ;;
        -c|--count)
            if (( $# < 2 )) || [[ ! "$2" =~ ^[0-9]+$ ]] || (( 10#$2 == 0 )); then
                echo "--count needs a whole number of verses, e.g. --count 5." >&2
                exit 1
            fi
            ROUNDS=$(( 10#$2 ))
            shift
            ;;
        -h|--help) usage; exit 0 ;;
        -*) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
        *) TSV="$1" ;;
    esac
    shift
done

if [[ ! -r "$TSV" ]]; then
    echo "Can't read verse file: $TSV" >&2
    exit 1
fi

if [[ -t 1 ]]; then
    BOLD=$'\e[1m' GREEN=$'\e[32m' YELLOW=$'\e[33m' RED=$'\e[31m' RESET=$'\e[0m'
else
    BOLD="" GREEN="" YELLOW="" RED="" RESET=""
fi

# Turn a book name into a comparable key, so "1 john", "1John", "First John"
# and "I John" all match the file's "I John".
normalize_book() {
    local name
    name="$(tr '[:upper:]' '[:lower:]' <<< "$1" | sed -E \
        -e 's/^[[:space:]]+//' \
        -e 's/^(1st|first|1)[[:space:]]*/i /' \
        -e 's/^(2nd|second|2)[[:space:]]*/ii /' \
        -e 's/^(3rd|third|3)[[:space:]]*/iii /' \
        -e 's/[^a-z]//g')"
    case "$name" in
        revelation|revelations) name="revelationofjohn" ;;
        songofsongs)            name="songofsolomon" ;;
        psalm)                  name="psalms" ;;
    esac
    printf '%s' "$name"
}

# Print the points for a numeric guess: max points minus penalty per number
# off, floored at 0. A guess that isn't a whole number scores 0.
closeness_points() {
    local guess="${1//[[:space:]]/}" answer=$2 max=$3 penalty=$4 error points
    if [[ ! "$guess" =~ ^[0-9]+$ ]]; then
        echo 0
        return
    fi
    error=$(( 10#$guess - answer ))
    (( error < 0 )) && error=$(( -error ))
    points=$(( max - error * penalty ))
    (( points < 0 )) && points=0
    echo "$points"
}

# Prompt for one answer and store it in ANSWER. Ctrl-D ends the quiz.
ask() {
    if ! read -r -p "$1" ANSWER; then
        echo
        echo "Quiz ended early."
        exit 0
    fi
}

# Split a one-line guess such as "1 John 1:9", "1 John 1 9", "John3:16" or
# "John 3" into GUESS_BOOK, GUESS_CHAPTER and GUESS_VERSE. Up to two trailing
# numbers are the chapter and verse; everything before them is the book.
# Missing parts are left empty.
parse_guess() {
    local spaced words=() nums=() n
    # Treat ":" and "." as separators, and split "John3" into "John 3".
    spaced="$(sed -E -e 's/[:.]/ /g' -e 's/([[:alpha:]])([0-9])/\1 \2/g' <<< "$1")"
    read -ra words <<< "$spaced"
    n=${#words[@]}
    while (( n > 0 && ${#nums[@]} < 2 )) && [[ "${words[n-1]}" =~ ^[0-9]+$ ]]; do
        nums=("${words[n-1]}" "${nums[@]}")
        n=$(( n - 1 ))
    done
    GUESS_BOOK="${words[*]:0:n}"
    GUESS_CHAPTER="${nums[0]:-}"
    GUESS_VERSE="${nums[1]:-}"
}

# ✓ for full points, ~ for partial, ✗ for none.
mark() {
    if (( $1 == $2 )); then printf '%s✓%s' "$GREEN" "$RESET"
    elif (( $1 > 0 )); then printf '%s~%s' "$YELLOW" "$RESET"
    else printf '%s✗%s' "$RED" "$RESET"
    fi
}

BOOK=""
case "$SCOPE" in
    all) SCOPE_LABEL="the whole Bible" ;;
    old) SCOPE_LABEL="the Old Testament" ;;
    new) SCOPE_LABEL="the New Testament" ;;
    book)
        key="$(normalize_book "$BOOK_ARG")"
        while read -r name; do
            if [[ "$(normalize_book "$name")" == "$key" ]]; then
                BOOK="$name"
                break
            fi
        done < <(tail -n +2 "$TSV" | cut -f1 | uniq)
        if [[ -z "$BOOK" ]]; then
            echo "No book named \"$BOOK_ARG\". Books in $TSV:" >&2
            tail -n +2 "$TSV" | cut -f1 | uniq | paste -sd',' | sed 's/,/, /g' | fold -s -w 76 >&2
            exit 1
        fi
        SCOPE_LABEL="$BOOK"
        CHAPTER_POINTS=$(( CHAPTER_POINTS + BOOK_POINTS ))
        BOOK_POINTS=0
        ;;
esac

# Skip the header and the verses the BSB leaves blank, then keep only the
# chosen testament or book. Books are numbered in file (canonical) order.
mapfile -t picks < <(awk -F'\t' -v scope="$SCOPE" -v book="$BOOK" -v ot="$OLD_TESTAMENT_BOOKS" '
    NR == 1 { next }
    $1 != prev { index_ += 1; prev = $1 }
    $4 == "" { next }
    scope == "old"  && index_ >  ot  { next }
    scope == "new"  && index_ <= ot  { next }
    scope == "book" && $1 != book    { next }
    { print }
' "$TSV" | shuf -n "$ROUNDS")

if (( ${#picks[@]} == 0 )); then
    echo "No verses found for $SCOPE_LABEL in $TSV." >&2
    exit 1
fi
if (( ${#picks[@]} < ROUNDS )); then
    echo "Only ${#picks[@]} verses available in $SCOPE_LABEL; using all of them." >&2
    ROUNDS=${#picks[@]}
fi

books=() chapters=() verses=() texts=()
raw_guesses=() book_guesses=() chapter_guesses=() verse_guesses=()
book_right=() chapter_scores=() verse_scores=()

echo "${BOLD}Verse Quiz: $SCOPE_LABEL${RESET}"
if [[ -n "$BOOK" ]]; then
    echo "Guess the chapter and verse for each of $ROUNDS verses from $BOOK."
    echo "Combo points:"
else
    echo "Guess the book, chapter, and verse for each of $ROUNDS verses."
    echo "Book: $BOOK_POINTS pt. Get the book right to earn combo points:"
fi
echo "  chapter up to $CHAPTER_POINTS pts (-$CHAPTER_PENALTY per number off),"
echo "  verse up to $VERSE_POINTS pts (-$VERSE_PENALTY per number off), only if the chapter is exact."

for i in "${!picks[@]}"; do
    IFS=$'\t' read -r book chapter verse text <<< "${picks[$i]}"
    books+=("$book") chapters+=("$chapter") verses+=("$verse") texts+=("$text")

    echo
    echo "${BOLD}Verse $((i + 1)) of $ROUNDS${RESET}"
    fold -s -w 76 <<< "$text" | sed 's/^/    /'
    echo

    if [[ -n "$BOOK" ]]; then
        ask "  Guess (Ch:V): "
        parse_guess "$ANSWER"
        book_guess="$book"   # Any book typed in single-book mode is ignored.
    else
        ask "  Guess (Bk Ch:V): "
        parse_guess "$ANSWER"
        book_guess="$GUESS_BOOK"
    fi
    chapter_guess="$GUESS_CHAPTER" verse_guess="$GUESS_VERSE"
    raw_guesses+=("$ANSWER")
    book_guesses+=("$book_guess") chapter_guesses+=("$chapter_guess") verse_guesses+=("$verse_guess")

    if [[ "$(normalize_book "$book_guess")" == "$(normalize_book "$book")" ]]; then
        book_right+=(1)
        chapter_score="$(closeness_points "$chapter_guess" "$chapter" "$CHAPTER_POINTS" "$CHAPTER_PENALTY")"
        chapter_scores+=("$chapter_score")
        if (( chapter_score == CHAPTER_POINTS )); then
            verse_scores+=("$(closeness_points "$verse_guess" "$verse" "$VERSE_POINTS" "$VERSE_PENALTY")")
        else
            verse_scores+=(0)
        fi
    else
        book_right+=(0) chapter_scores+=(0) verse_scores+=(0)
    fi
done

echo
echo "${BOLD}=== Results ===${RESET}"

score=0 combo_total=0 books_correct=0
for i in "${!books[@]}"; do
    book_score=$(( book_right[i] * BOOK_POINTS ))
    combo=$(( chapter_scores[i] + verse_scores[i] ))
    points=$(( book_score + combo ))
    (( score += points, combo_total += combo, books_correct += book_right[i] )) || true

    echo
    echo "${BOLD}$((i + 1)). ${books[i]} ${chapters[i]}:${verses[i]}${RESET}  (+$points)"
    fold -s -w 76 <<< "${texts[i]}" | sed 's/^/    /'
    echo "    Your guess: ${raw_guesses[i]:-?}"
    if [[ -z "$BOOK" ]]; then
        printf '    Book:    %s %s  (+%d)\n' "$(mark "$book_score" "$BOOK_POINTS")" \
            "${book_guesses[i]:-?}" "$book_score"
    fi
    if (( book_right[i] )); then
        printf '    Chapter: %s %s  (+%d)\n' "$(mark "${chapter_scores[i]}" "$CHAPTER_POINTS")" \
            "${chapter_guesses[i]:-?}" "${chapter_scores[i]}"
        if (( chapter_scores[i] == CHAPTER_POINTS )); then
            printf '    Verse:   %s %s  (+%d)\n' "$(mark "${verse_scores[i]}" "$VERSE_POINTS")" \
                "${verse_guesses[i]:-?}" "${verse_scores[i]}"
        else
            echo "    Verse:   ${verse_guesses[i]:-?}"
        fi
        echo "    Combo points: $combo"
    else
        echo "    Chapter: ${chapter_guesses[i]:-?}, Verse: ${verse_guesses[i]:-?}"
    fi
done

max_combo=$(( ROUNDS * (CHAPTER_POINTS + VERSE_POINTS) ))
max_score=$(( ROUNDS * BOOK_POINTS + max_combo ))
echo
if [[ -z "$BOOK" ]]; then
    echo "Books correct: $books_correct / $ROUNDS"
fi
echo "Combo points:  $combo_total"
echo "${BOLD}Score: $score${RESET}"
