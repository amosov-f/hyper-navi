<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">

    <title>Admin</title>

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>

    <script src="https://api-maps.yandex.ru/2.1/?lang=ru_RU" type="text/javascript"></script>

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css">

    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            padding: 0;
            margin: 0;
        }
        .popover {
            display: block;
            max-width: 600px;
        }
        .close {
            position: absolute;
            right: 5px;
            top: 1px;
        }
    </style>
</head>
<body>

<div id="map"/>

<script src="/web/pages/balloon.js" type="text/javascript"></script>
<script src="/web/pages/admin.js" type="text/javascript"></script>

</body>
</html>
