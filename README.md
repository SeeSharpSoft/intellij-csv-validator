[![Build Status](https://travis-ci.org/SeeSharpSoft/intellij-csv-validator.svg?branch=master)](https://travis-ci.org/SeeSharpSoft/intellij-csv-validator)
[![Coverage Status](https://coveralls.io/repos/github/SeeSharpSoft/intellij-csv-validator/badge.svg?branch=master)](https://coveralls.io/github/SeeSharpSoft/intellij-csv-validator?branch=master)
# Lightweight CSV Plugin for JetBrains IDE family

Compatible with _IntelliJ IDEA  PhpStorm  WebStorm  PyCharm  RubyMine  AppCode  CLion  Gogland  DataGrip  Rider  MPS  Android Studio_ - __2016.3.2 and newer__

This plugin introduces CSV (_Comma-Separated Values_) as a language to Jetbrains IDE with a syntax definition, structured language elements and associated file types (.csv/.tsv).
This enables default editor features like syntax validation, highlighting and inspections for CSV files.

## Features

- CSV/TSV file detection
- syntax validation
- syntax highlighting (configurable)
- content formatting (configurable)
- quick fix inspections
- intentions (Alt+Enter), e.g. Quote/Unquote (all), Shift Column Left/Right
- structure view (header-entry layout)
- support for ',', ';', '|' and '&#8633;' as value separator

### Syntax parser & validation

The CSV syntax parser follows the standard defined in [IETF 4180](https://www.ietf.org/rfc/rfc4180.txt) but tolerates leading and trailing whitespaces of escaped text and accepts basically every literal as text data.
This results in a less restrictive checks and contributes to the flexibility of this format.
The goal of the plugin is to support editing files in CSV format, not introducing new hurdles.
 
Being strict, the following CSV snippet is actually incorrect cause of the leading whitespaces. However, it is accepted by the plugins syntax parser implementation:

```
"firstName", "lastName", "birthday"
```
Besides the mentioned diversion from the standard definition, syntax errors will be detected and can be inspected.
Please note that if a document is syntactically incorrect, other features like code formatting or the structure view can not function properly.

![Editor with syntax validation and highlighting](./docs/editor.png)

### Highlighting

The different symbols of a CSV document, namely the separator (comma), the quotes, the escaped literals and the text elements itself, are highlighted by a coloring scheme that can be customized:

- _File > Settings > Editor > Color Scheme > CSV_

Preset colors are based on Jetbrains IDE defaults and support the different UI themes.

![Color scheme settings](./docs/colorsettings.png)

### Separator

CSV files provide a high degree of flexibility and can be used universally for all kind of data.
This led to a variety of CSV derivatives like semicolon or pipe separated values, which share the common format but make use of a different separator.

The plugin supports project specific separator setting.
New separators can be added fairly easy in the parser definition of the source code.

#### TSV

Tab (&#8633;) can be explicitly set as a separator for CSV files.
Additionally the file type TSV was introduced as a kind of CSV language.
For TSV files the same formatter and code style settings are applied as for CSV itself, but the separator is considered to be a tab.
All functionality that is available for plain CSV files (inspections, intentions, structure view, etc.) can be used for TSV as well.   

### Formatting

- _File > Settings > Editor > Code Style > CSV_

Formatting CSV is tricky: On one hand it is easy cause the language has only four different symbols and is generally easy to handle.
On the other hand, formatting elements like whitespaces and tabs do have a meaning in CSV.
However, in reality when parsing CSV, the leading and trailing whitespaces are quite often ignored or trimmed.
Formatting can be completely disabled if no option is selected at all at the settings screen.

![Format settings](./docs/codestyle.png)

#### Formatting options

The formatting examples are based on the following CSV snippet as input:

```
"name", "city", "position"
Annasusanna,Amsterdam,1
  Ben  ,  Berlin  , 2
```

##### Separator

The following separators are currently supported: ',', ';', '|' and '&#8633;'

When changing the separator, press the apply button to refresh the preview window properly. 

_Space before separator_

```
"name" , "city" , "position"
Annasusanna ,Amsterdam ,1
  Ben ,  Berlin , 2
```

_Space after separator_

```
"name", "city", "position"
Annasusanna, Amsterdam, 1
  Ben  , Berlin  , 2
```

##### Trimming

Trimming can be combined with _Space before/after separator_.

_Trim leading whitespaces_

```
"name","city","position"
Annasusanna,Amsterdam,1
Ben  ,Berlin  ,2
```

_Trim trailing whitespaces_

```
"name", "city", "position"
Annasusanna,Amsterdam,1
  Ben,  Berlin, 2
```

##### Tabularize enabled _(default)_

Please note: The separator settings can be used in combination with Tabularize enabled, while trimming options are ignored.

```
"name     ","city   ","position"
Annasusanna,Amsterdam,1
Ben        ,Berlin   ,2
```

_Trimming/spacing outside quotes_

```
"name"     ,"city"   ,"position"
Annasusanna,Amsterdam,1
Ben        ,Berlin   ,2
```

_Leading whitespaces_

```
"     name","   city","position"
Annasusanna,Amsterdam,         1
        Ben,   Berlin,         2
```

_Trimming/spacing outside quotes & Leading whitespaces_

```
     "name",   "city","position"
Annasusanna,Amsterdam,         1
        Ben,   Berlin,         2
```

### Inspections

- _File > Settings > Editor > Inspections > CSV_ 

Inspections are an IDE feature that can be used to fix syntax errors.
They are accessed via _Alt+Enter_ when the cursor is at an erroneous position.
The plugin provides three types of inspections:

- **Surround with quotes** Surrounds the current field with quotes and escapes already existing ones
- **Add separator** Adds a (missing) separator at the cursor position
- **Add closing quote** Adds a (missing) closing quote at the end of the document

### Intentions

- _File > Settings > Editor > Intentions > CSV_

Intentions are similar to inspections and provide a quick way to automatically adjust the document.
They are accessed via _Alt+Enter_ at any time. The shown intentions can vary depending on the cursor position within the document. 
The plugin provides six types of intentions:

- **Quote All** Surrounds all unquoted fields with quotes
- **Quote Value** Quotes the currently unquoted value at the cursor position
- **Unquote All** Unquotes all quoted fields if possible
- **Unquote Value** Unquotes the currently quoted value at the cursor position if possible
- **Shift Column Left** Shifts the column at cursor position to the left
- **Shift Column Right** Shifts the column at cursor position to the right

### Structure View

- _View > Tool Windows > Structure_

The structure view shows the first line of the currently opened CSV file as header.
Expanding a header entry shows all entries in this column.

![Structure view](./docs/structureview.png)

## Installation

Install it from the Jetbrains plugin repository within your IDE (**recommended**):

- _File > Settings > Plugins > Browse repositories... > Search 'CSV Plugin' > Category 'Editor'_

You can also download the JAR package from the [Jetbrains plugin repository](https://plugins.jetbrains.com/plugin/10037-csv-plugin) or from [GitHub Releases](https://github.com/SeeSharpSoft/intellij-csv-validator/releases) and add it manually to your plugins:

- _File > Settings > Plugins > Install plugin from disk..._

## Contribution

Contributions are welcome. Please check [CONTRIBUTING.md](./CONTRIBUTING.md) for more information.

Besides source code contributions, feel free to open bug reports or just suggest new features [here](https://github.com/SeeSharpSoft/intellij-csv-validator/issues).

## FAQ

> Where is the table editor?

The initial intention of this plugin was the usage of the built-in text-editor features (e.g. validation, highlighting, formatting, etc) for CSV files.
Providing an own editor was not planned at all.
Still, it's just a matter of time and effort that needs to be invested for the implementation.
Feel free to contribute to [make this dream come true](https://github.com/SeeSharpSoft/intellij-csv-validator/issues/2).

Please note that there is a table like editor available in [IntelliJ IDEA Ultimate/PhpStorm/DataGrip/etc.](https://www.jetbrains.com/help/phpstorm/editing-csv-and-tsv-files.html)
It is a well thought-through component and worth giving it a try if a CSV table editor is a necessity in your daily work.
This plugin is fully compatible with this feature.

> Why can't I choose the separator freely?

Having clearly defined symbols enables the syntax parser and language lexer to do its job properly.
The code for those is generated during build time by using the [Grammar-Kit](https://github.com/JetBrains/Grammar-Kit).
Adding a new kind of separator during development is fairly easy (please feel free to request a new commonly used one) in comparison to the implementation effort and usefulness of a freely defined separator. 


## Jetbrains Repository

JetBrains Plugin Repository Link: https://plugins.jetbrains.com/plugin/10037-csv-plugin
