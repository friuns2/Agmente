package com.agmente.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ServerDao_Impl implements ServerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServerEntity> __insertionAdapterOfServerEntity;

  private final EntityDeletionOrUpdateAdapter<ServerEntity> __deletionAdapterOfServerEntity;

  private final EntityDeletionOrUpdateAdapter<ServerEntity> __updateAdapterOfServerEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public ServerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServerEntity = new EntityInsertionAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `servers` (`id`,`name`,`scheme`,`host`,`token`,`cfAccessClientId`,`cfAccessClientSecret`,`workingDirectory`,`serverType`,`usedWorkingDirectories`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getScheme());
        statement.bindString(4, entity.getHost());
        statement.bindString(5, entity.getToken());
        statement.bindString(6, entity.getCfAccessClientId());
        statement.bindString(7, entity.getCfAccessClientSecret());
        statement.bindString(8, entity.getWorkingDirectory());
        statement.bindString(9, entity.getServerType());
        statement.bindString(10, entity.getUsedWorkingDirectories());
      }
    };
    this.__deletionAdapterOfServerEntity = new EntityDeletionOrUpdateAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `servers` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfServerEntity = new EntityDeletionOrUpdateAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `servers` SET `id` = ?,`name` = ?,`scheme` = ?,`host` = ?,`token` = ?,`cfAccessClientId` = ?,`cfAccessClientSecret` = ?,`workingDirectory` = ?,`serverType` = ?,`usedWorkingDirectories` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getScheme());
        statement.bindString(4, entity.getHost());
        statement.bindString(5, entity.getToken());
        statement.bindString(6, entity.getCfAccessClientId());
        statement.bindString(7, entity.getCfAccessClientSecret());
        statement.bindString(8, entity.getWorkingDirectory());
        statement.bindString(9, entity.getServerType());
        statement.bindString(10, entity.getUsedWorkingDirectories());
        statement.bindString(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM servers WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ServerEntity server, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerEntity.insert(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ServerEntity server, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfServerEntity.handle(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final ServerEntity server, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfServerEntity.handle(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<ServerEntity>> $completion) {
    final String _sql = "SELECT * FROM servers ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ServerEntity>>() {
      @Override
      @NonNull
      public List<ServerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "scheme");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfCfAccessClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "cfAccessClientId");
          final int _cursorIndexOfCfAccessClientSecret = CursorUtil.getColumnIndexOrThrow(_cursor, "cfAccessClientSecret");
          final int _cursorIndexOfWorkingDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "workingDirectory");
          final int _cursorIndexOfServerType = CursorUtil.getColumnIndexOrThrow(_cursor, "serverType");
          final int _cursorIndexOfUsedWorkingDirectories = CursorUtil.getColumnIndexOrThrow(_cursor, "usedWorkingDirectories");
          final List<ServerEntity> _result = new ArrayList<ServerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpScheme;
            _tmpScheme = _cursor.getString(_cursorIndexOfScheme);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final String _tmpToken;
            _tmpToken = _cursor.getString(_cursorIndexOfToken);
            final String _tmpCfAccessClientId;
            _tmpCfAccessClientId = _cursor.getString(_cursorIndexOfCfAccessClientId);
            final String _tmpCfAccessClientSecret;
            _tmpCfAccessClientSecret = _cursor.getString(_cursorIndexOfCfAccessClientSecret);
            final String _tmpWorkingDirectory;
            _tmpWorkingDirectory = _cursor.getString(_cursorIndexOfWorkingDirectory);
            final String _tmpServerType;
            _tmpServerType = _cursor.getString(_cursorIndexOfServerType);
            final String _tmpUsedWorkingDirectories;
            _tmpUsedWorkingDirectories = _cursor.getString(_cursorIndexOfUsedWorkingDirectories);
            _item = new ServerEntity(_tmpId,_tmpName,_tmpScheme,_tmpHost,_tmpToken,_tmpCfAccessClientId,_tmpCfAccessClientSecret,_tmpWorkingDirectory,_tmpServerType,_tmpUsedWorkingDirectories);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String id, final Continuation<? super ServerEntity> $completion) {
    final String _sql = "SELECT * FROM servers WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ServerEntity>() {
      @Override
      @Nullable
      public ServerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "scheme");
          final int _cursorIndexOfHost = CursorUtil.getColumnIndexOrThrow(_cursor, "host");
          final int _cursorIndexOfToken = CursorUtil.getColumnIndexOrThrow(_cursor, "token");
          final int _cursorIndexOfCfAccessClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "cfAccessClientId");
          final int _cursorIndexOfCfAccessClientSecret = CursorUtil.getColumnIndexOrThrow(_cursor, "cfAccessClientSecret");
          final int _cursorIndexOfWorkingDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "workingDirectory");
          final int _cursorIndexOfServerType = CursorUtil.getColumnIndexOrThrow(_cursor, "serverType");
          final int _cursorIndexOfUsedWorkingDirectories = CursorUtil.getColumnIndexOrThrow(_cursor, "usedWorkingDirectories");
          final ServerEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpScheme;
            _tmpScheme = _cursor.getString(_cursorIndexOfScheme);
            final String _tmpHost;
            _tmpHost = _cursor.getString(_cursorIndexOfHost);
            final String _tmpToken;
            _tmpToken = _cursor.getString(_cursorIndexOfToken);
            final String _tmpCfAccessClientId;
            _tmpCfAccessClientId = _cursor.getString(_cursorIndexOfCfAccessClientId);
            final String _tmpCfAccessClientSecret;
            _tmpCfAccessClientSecret = _cursor.getString(_cursorIndexOfCfAccessClientSecret);
            final String _tmpWorkingDirectory;
            _tmpWorkingDirectory = _cursor.getString(_cursorIndexOfWorkingDirectory);
            final String _tmpServerType;
            _tmpServerType = _cursor.getString(_cursorIndexOfServerType);
            final String _tmpUsedWorkingDirectories;
            _tmpUsedWorkingDirectories = _cursor.getString(_cursorIndexOfUsedWorkingDirectories);
            _result = new ServerEntity(_tmpId,_tmpName,_tmpScheme,_tmpHost,_tmpToken,_tmpCfAccessClientId,_tmpCfAccessClientSecret,_tmpWorkingDirectory,_tmpServerType,_tmpUsedWorkingDirectories);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
