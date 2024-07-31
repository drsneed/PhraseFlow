
# Sample list of 100 five-letter words
words = [
    "apple", "brave", "crane", "dance", "eagle", "flame", "grape", "heart", "ivory", "joker",
    "knock", "lemon", "mango", "night", "ocean", "pearl", "quilt", "robin", "stone", "tiger",
    "vivid", "whale", "xenon", "yacht", "zebra", "actor", "beach", "charm", "doubt", "eagle",
    "frost", "globe", "haste", "input", "jumpy", "koala", "lunar", "march", "noble", "order",
    "plumb", "quest", "rusty", "sail", "train", "usual", "vivid", "waste", "xenon", "youth",
    "zesty", "alone", "beard", "candy", "drain", "enjoy", "flair", "goose", "honey", "ideal",
    "jolly", "knock", "loyal", "mirth", "nurse", "olive", "piano", "quest", "rapid", "sworn",
    "teeth", "under", "vowel", "water", "xenon", "yacht", "zebra", "adore", "bacon", "crisp",
    "dwell", "early", "fever", "glide", "happy", "ideal", "joker", "kiosk", "lemon", "medal"
]

# Write words to file
with open('word_list.txt', 'w') as file:
    file.write('\n'.join(words))
