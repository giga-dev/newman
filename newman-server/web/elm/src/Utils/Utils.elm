module Utils.Utils exposing (..)

import Task

(=>) : d -> (d->msg) -> Cmd msg
(=>) d msg =
    Task.perform msg  <| Task.succeed <| d