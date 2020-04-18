initial:BEGIN_QUOTE:quoted_text
initial:TEXT:unquoted_text
initial:SEPARATOR|LINE_FEED:initial

quoted_text:QUOTED_TEXT:quoted_text
quoted_text:END_QUOTE:initial

unquoted_text:TEXT:unquoted_text
unquoted_text:SEPARATOR|LINE_FEED:initial

BEGIN_QUOTE= *"
QUOTED_TEXT=([^"]+|"")+
END_QUOTE=(?<!")" *
SEPARATOR=,
TEXT=[^",\n]+
LINE_FEED=\n